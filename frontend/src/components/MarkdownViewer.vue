<template>
  <div class="markdown-viewer" v-html="renderedHtml" />
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  content?: string
  /** 空内容时显示的占位文字 */
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  placeholder: '暂无内容',
})

/**
 * 轻量 Markdown 渲染器（无外部依赖）
 * 支持：标题 # ## ###、加粗 **、斜体 *、行内代码 `、代码块 ```、列表 -/1.、分割线 ---
 */
const renderedHtml = computed(() => {
  const raw = props.content?.trim()
  if (!raw) {
    return `<p class="md-placeholder">${escHtml(props.placeholder)}</p>`
  }
  return renderMarkdown(raw)
})

function escHtml(str: string) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderMarkdown(md: string): string {
  const lines = md.split('\n')
  const result: string[] = []
  let inCodeBlock = false
  let codeLang = ''
  let codeLines: string[] = []
  let inList = false
  let listOrdered = false

  const flushList = () => {
    if (!inList) return
    result.push(listOrdered ? '</ol>' : '</ul>')
    inList = false
  }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    // 代码块
    if (line.startsWith('```')) {
      if (!inCodeBlock) {
        flushList()
        inCodeBlock = true
        codeLang = line.slice(3).trim()
        codeLines = []
      } else {
        const langClass = codeLang ? ` class="language-${escHtml(codeLang)}"` : ''
        result.push(`<pre><code${langClass}>${codeLines.map(escHtml).join('\n')}</code></pre>`)
        inCodeBlock = false
        codeLang = ''
        codeLines = []
      }
      continue
    }
    if (inCodeBlock) {
      codeLines.push(line)
      continue
    }

    // 分割线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) {
      flushList()
      result.push('<hr />')
      continue
    }

    // 标题
    const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
    if (headingMatch) {
      flushList()
      const level = headingMatch[1].length
      result.push(`<h${level}>${renderInline(headingMatch[2])}</h${level}>`)
      continue
    }

    // 有序列表
    const olMatch = line.match(/^(\d+)\.\s+(.+)$/)
    if (olMatch) {
      if (!inList || !listOrdered) {
        flushList()
        result.push('<ol>')
        inList = true
        listOrdered = true
      }
      result.push(`<li>${renderInline(olMatch[2])}</li>`)
      continue
    }

    // 无序列表
    const ulMatch = line.match(/^[-*+]\s+(.+)$/)
    if (ulMatch) {
      if (!inList || listOrdered) {
        flushList()
        result.push('<ul>')
        inList = true
        listOrdered = false
      }
      result.push(`<li>${renderInline(ulMatch[1])}</li>`)
      continue
    }

    // 普通段落
    flushList()
    if (line.trim() === '') {
      result.push('<br />')
    } else {
      result.push(`<p>${renderInline(line)}</p>`)
    }
  }

  flushList()
  return result.join('')
}

/** 渲染行内元素：加粗、斜体、行内代码、链接 */
function renderInline(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/_([^_]+)_/g, '<em>$1</em>')
}
</script>

<style scoped>
.markdown-viewer {
  line-height: 1.75;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.markdown-viewer :deep(h1),
.markdown-viewer :deep(h2),
.markdown-viewer :deep(h3),
.markdown-viewer :deep(h4),
.markdown-viewer :deep(h5),
.markdown-viewer :deep(h6) {
  font-weight: 600;
  margin: 1em 0 0.5em;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}

.markdown-viewer :deep(h1) { font-size: 1.6em; border-bottom: 2px solid var(--el-border-color); padding-bottom: 0.3em; }
.markdown-viewer :deep(h2) { font-size: 1.35em; border-bottom: 1px solid var(--el-border-color-light); padding-bottom: 0.2em; }
.markdown-viewer :deep(h3) { font-size: 1.15em; }

.markdown-viewer :deep(p) {
  margin: 0.5em 0;
}

.markdown-viewer :deep(ul),
.markdown-viewer :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.markdown-viewer :deep(li) {
  margin: 0.2em 0;
}

.markdown-viewer :deep(code) {
  background: var(--el-fill-color-light);
  border-radius: 3px;
  padding: 0.15em 0.4em;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.88em;
  color: var(--el-color-danger);
}

.markdown-viewer :deep(pre) {
  background: var(--el-fill-color);
  border-radius: 6px;
  padding: 1em 1.2em;
  overflow-x: auto;
  margin: 0.8em 0;
}

.markdown-viewer :deep(pre code) {
  background: none;
  padding: 0;
  color: var(--el-text-color-primary);
  font-size: 0.88em;
}

.markdown-viewer :deep(hr) {
  border: none;
  border-top: 1px solid var(--el-border-color);
  margin: 1em 0;
}

.markdown-viewer :deep(strong) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.markdown-viewer :deep(em) {
  font-style: italic;
}

.markdown-viewer :deep(.md-placeholder) {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}
</style>
