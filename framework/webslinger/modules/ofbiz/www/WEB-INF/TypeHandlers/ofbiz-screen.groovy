import org.ofbiz.base.util.StringUtil
import org.ofbiz.base.util.UtilXml
import org.ofbiz.base.util.collections.MapStack
import org.ofbiz.base.util.template.FreeMarkerWorker
import org.ofbiz.widget.screen.ScreenFactory
import org.ofbiz.widget.screen.ScreenRenderer
import org.ofbiz.widget.html.HtmlFormRenderer
import org.ofbiz.widget.html.HtmlScreenRenderer

import org.webslinger.commons.vfs.VFSUtil
import org.webslinger.container.FileInfo

switch (webslinger.command) {
    case 'init':
        return new HtmlScreenRenderer()
    case 'get-creator':
        return [
            createValue: { fi, name ->
                def file = fi.file
                def singleScreenText = VFSUtil.getString(file)
                def screenName = fi.servletFile.name.path
                def fullDocumentText = '<screens xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://ofbiz.apache.org/dtds/widget-screen.xsd"><screen name="' + screenName + '">' + singleScreenText + '</screen></screens>'
                def doc = UtilXml.readXmlDocument(fullDocumentText)
                def screens = ScreenFactory.readScreenDocument(doc, "webslinger://$webslinger.webslingerServletContext.id")
                if (screens.size() != 1) throw new IllegalArgumentException('wrong size')
                return screens.values().iterator().next()
            }
        ] as FileInfo.Creator
}
def target = webslinger.payload
def modelScreen = target.pathContext.info.getCachedItem(null)
def screenRenderer = webslinger.initObject
//response.contentType = 'text/html'
def targetContext = MapStack.create(target.context)
def screens = new ScreenRenderer(response.writer, targetContext, screenRenderer);
screens.populateContextForRequest(request, response, webslinger.webslingerServletContext)
FreeMarkerWorker.getSiteParameters(request, targetContext)
targetContext.formStringRenderer = new HtmlFormRenderer(request, response)
targetContext.simpleEncoder = StringUtil.htmlEncoder
modelScreen.renderScreenString(response.writer, targetContext, screenRenderer)
return null
