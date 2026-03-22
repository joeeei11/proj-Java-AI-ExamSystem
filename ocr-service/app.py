"""
OCR 微服务 - Flask + PaddleOCR
端口：5001
接口：POST /ocr  { "imageBase64": "..." } -> { "text": "...", "blocks": [...] }
"""

import base64
import io
import logging
import os
import tempfile
from functools import lru_cache

from flask import Flask, jsonify, request
from PIL import Image

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# ===== PaddleOCR 懒加载（首次调用时初始化，避免启动过慢）=====

@lru_cache(maxsize=1)
def get_ocr():
    """单例加载 PaddleOCR 模型"""
    from paddleocr import PaddleOCR  # type: ignore

    logger.info("正在加载 PaddleOCR 模型...")
    ocr = PaddleOCR(
        use_angle_cls=True,
        lang="ch",
        use_gpu=False,
        show_log=False,
    )
    logger.info("PaddleOCR 模型加载完成")
    return ocr


def decode_base64_image(image_base64: str) -> str:
    """将 Base64 图片解码并保存为临时文件，返回文件路径"""
    # 去掉 data URI 前缀（如 data:image/png;base64,）
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    image_data = base64.b64decode(image_base64)
    image = Image.open(io.BytesIO(image_data))

    # 转换为 RGB（PaddleOCR 需要）
    if image.mode != "RGB":
        image = image.convert("RGB")

    # 写入临时文件
    tmp = tempfile.NamedTemporaryFile(suffix=".png", delete=False)
    image.save(tmp.name, "PNG")
    return tmp.name


@app.route("/health", methods=["GET"])
def health():
    """健康检查"""
    return jsonify({"status": "UP", "service": "ocr-service"})


@app.route("/ocr", methods=["POST"])
def ocr():
    """
    识别图片文字

    请求体：{ "imageBase64": "<base64字符串>" }
    响应：  { "text": "<识别文本>", "blocks": [{ "text": "...", "confidence": 0.99 }] }
    """
    try:
        payload = request.get_json(force=True)
        if not payload or "imageBase64" not in payload:
            return jsonify({"error": "缺少 imageBase64 字段"}), 400

        image_base64 = payload["imageBase64"]
        if not image_base64:
            return jsonify({"error": "imageBase64 不能为空"}), 400

        # 解码图片
        image_path = None
        try:
            image_path = decode_base64_image(image_base64)

            # 执行 OCR
            ocr_engine = get_ocr()
            result = ocr_engine.ocr(image_path, cls=True)

            # 解析结果
            blocks = []
            lines = []

            if result and result[0]:
                for line in result[0]:
                    if line and len(line) >= 2:
                        box, (text, confidence) = line[0], line[1]
                        blocks.append({
                            "text": text,
                            "confidence": round(float(confidence), 4),
                            "box": box,
                        })
                        lines.append(text)

            full_text = "\n".join(lines)
            logger.info("OCR 识别完成，共 %d 个文本块", len(blocks))

            return jsonify({"text": full_text, "blocks": blocks})

        finally:
            # 清理临时文件
            if image_path and os.path.exists(image_path):
                os.unlink(image_path)

    except Exception as e:
        logger.error("OCR 识别失败: %s", str(e), exc_info=True)
        return jsonify({"error": f"OCR 识别失败: {str(e)}"}), 500


if __name__ == "__main__":
    port = int(os.environ.get("OCR_PORT", 5001))
    logger.info("OCR 微服务启动，端口: %d", port)
    app.run(host="0.0.0.0", port=port, debug=False)
