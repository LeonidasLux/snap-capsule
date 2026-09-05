package com.snapcapsule.platform

import com.snapcapsule.ui.CapBridge
import com.snapcapsule.ui.ImportDispatcher
import com.snapcapsule.ui.ToastPresenter

actual fun installPlatformHandlers() {
    // 导出：浏览器下载 JSON 文件
    CapBridge.onExportJson = { fileName, text ->
        val download: dynamic = js(
            """(function (text, fileName) {
                  var blob = new Blob([text], {type: 'application/json'});
                  var url = URL.createObjectURL(blob);
                  var a = document.createElement('a');
                  a.href = url;
                  a.download = fileName;
                  document.body.appendChild(a);
                  a.click();
                  setTimeout(function () { a.remove(); URL.revokeObjectURL(url); }, 200);
              })"""
        )
        download(text, fileName)
    }

    // 导入：隐藏 <input type=file> + FileReader，内容交 ImportDispatcher 由 UI 统一消费
    CapBridge.onPickImportFile = {
        val input: dynamic = js(
            """(function () {
                  var i = document.createElement('input');
                  i.type = 'file';
                  i.accept = '.json,application/json';
                  return i;
              })()"""
        )
        input.onchange = { evt: dynamic ->
            val file: dynamic = evt.target.files[0]
            val reader: dynamic = js("new FileReader()")
            reader.onload = { _: dynamic ->
                val content: String = (reader.result as String?) ?: ""
                ImportDispatcher.pendingText = content
            }
            reader.readAsText(file)
        }
        input.click()
    }

    // Toast：独立 fixed 元素 + pointer-events:none —— 浮在所有弹层之上且绝不拦截点击。
    // 这是关键：用 Compose Dialog 画提示会开一个全屏模态窗口，弹出期间整页都点不了。
    ToastPresenter.show = { msg -> showTopToast(msg) }
}

/** 顶部 Toast（H5 自绘，样式对齐 Compose 版本：暖黑底白字圆角，置于顶部 48px）。 */
private fun showTopToast(msg: String) {
    val show: dynamic = js(
        """(function (msg) {
              var prev = document.getElementById('sc-toast');
              if (prev && prev.parentNode) prev.parentNode.removeChild(prev);
              var d = document.createElement('div');
              d.id = 'sc-toast';
              d.setAttribute('role', 'status');
              d.textContent = msg;
              d.style.cssText = [
                'position:fixed','top:48px','left:50%','transform:translateX(-50%)',
                'max-width:calc(100vw - 32px)','box-sizing:border-box',
                'background:#2A2620','color:#FFFFFF','font-size:13px','line-height:1.5',
                'padding:11px 18px','border-radius:12px','z-index:2147483000',
                'pointer-events:none','white-space:nowrap','overflow:hidden','text-overflow:ellipsis',
                'text-align:center','font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif'
              ].join(';');
              document.body.appendChild(d);
              setTimeout(function () { if (d.parentNode) d.parentNode.removeChild(d); }, 1900);
          })"""
    )
    show(msg)
}
