import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.renderer.color.IAtomColorer
import org.openscience.cdk.renderer.color.CDK2DAtomColors

import java.awt.Color
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*
import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.util.regex.*
import javax.xml.transform.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class Highlight {

	static final class molpicColorer implements IAtomColorer {
		static Map<String, Color> pallet = [
			N  : new Color(51, 51, 153),
			O  : new Color(255, 0, 0),
			F  : new Color(153, 102, 0),
			B  : new Color(0, 153, 0),
			Cl : new Color(0, 153, 0),
			P  : new Color(153, 102, 0),
			S  : new Color(102, 102, 0),
			Br : new Color(102, 51, 51),
			Si : new Color(153, 102, 0)
		]

		@Override
		Color getAtomColor(IAtom atom, Color color) {
			pallet.getOrDefault(atom.symbol, color)
		}

		@Override
		Color getAtomColor(IAtom atom) {
			getAtomColor(atom, Color.BLACK)
		}
	}

	static String modifySVG(String svgInput) {
		try {
			// Parse SVG
			def factory = DocumentBuilderFactory.newInstance()
			factory.namespaceAware = false
			factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
			def builder = factory.newDocumentBuilder()
			def doc = builder.parse(new InputSource(new StringReader(svgInput)))

			// XPath setup
			def xpath = XPathFactory.newInstance().newXPath()

			// Collect paths with non-black fill
			def pathNodes = xpath.compile("//*[@d]").evaluate(doc, XPathConstants.NODESET) as NodeList
			def pathsWithPositions = []
			for (int i = 0; i < pathNodes.length; i++) {
				def path = pathNodes.item(i) as Element
				def fill = getEffectiveFill(path)
				if (fill && fill.toLowerCase() != "black" && fill != "#000000" && fill != "none") {
					def pos = getPathStartingPoint(path)
					if (pos) pathsWithPositions << [path: path, fill: fill, pos: pos]
				}
			}

			// Collect lines
			def lineNodes = xpath.compile("//*[@x1]").evaluate(doc, XPathConstants.NODESET) as NodeList
			def linesWithPositions = []
			for (int i = 0; i < lineNodes.length; i++) {
				def line = lineNodes.item(i) as Element
				linesWithPositions << [
					line: line,
					p1  : [line.getAttribute("x1").toDouble(), line.getAttribute("y1").toDouble()],
					p2  : [line.getAttribute("x2").toDouble(), line.getAttribute("y2").toDouble()]
				]
			}

			double threshold = 7.77

			linesWithPositions.each { lineInfo ->
				def line = lineInfo.line
				def (p1, p2) = [lineInfo.p1, lineInfo.p2]
				double dx = p2[0] - p1[0]
				double dy = p2[1] - p1[1]
				double lengthSquared = dx*dx + dy*dy
				if (lengthSquared == 0) return

				pathsWithPositions.each { pathInfo ->
					def path = pathInfo.path
					def fill = pathInfo.fill
					def (x, y) = pathInfo.pos

					double w_x = x - p1[0]
					double w_y = y - p1[1]
					double t = (w_x*dx + w_y*dy)/lengthSquared
					double t_clamped = t < 0 ? 0 : (t > 1 ? 1 : t)
					double closest_x = p1[0] + t_clamped*dx
					double closest_y = p1[1] + t_clamped*dy
					double dist_to_line = Math.hypot(x - closest_x, y - closest_y)

					if (dist_to_line < threshold) {
						def bondParent = line.parentNode as Element
						if (bondParent?.tagName == "g" && bondParent.getAttribute("class") == "bond") {
							def count = 0
							bondParent.childNodes.each { n ->
								if (n instanceof Element && n.getAttribute("class") != "hi") count++
							}
							if (count > 3) {
								boolean isFirst = true
								bondParent.childNodes.each { n ->
									if (n instanceof Element) {
										n.setAttribute("stroke", isFirst ? "#000000" : fill)
										isFirst = false
									}
								}
								return
							}
						}

						// Add new line
						def mid_x = (p1[0] + p2[0])/2
						def mid_y = (p1[1] + p2[1])/2
						def newLine = line.cloneNode(false) as Element
						newLine.setAttribute("class", "hi")
						newLine.setAttribute("x1", closest_x.toString())
						newLine.setAttribute("y1", closest_y.toString())
						newLine.setAttribute("x2", mid_x.toString())
						newLine.setAttribute("y2", mid_y.toString())
						newLine.setAttribute("stroke", fill)
						line.parentNode.appendChild(newLine)
					}
				}
			}

			// Serialize back to string
			def transformer = TransformerFactory.newInstance().newTransformer()
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
			transformer.setOutputProperty(OutputKeys.INDENT, "yes")
			def writer = new StringWriter()
			transformer.transform(new DOMSource(doc), new StreamResult(writer))
			return writer.toString()

		} catch (Exception e) {
			e.printStackTrace()
			return null
		}
	}

	private static String getEffectiveFill(Element element) {
		while (element) {
			def fill = element.getAttribute("fill")
			if (fill) return fill
			def parent = element.parentNode
			element = (parent instanceof Element) ? parent : null
		}
		return null
	}

	private static double[] getPathStartingPoint(Element path) {
		def d = path.getAttribute("d")
		if (d) {
			def matcher = Pattern.compile(/M\s*([+-]?\d*\.?\d+)\s*,?\s*([+-]?\d*\.?\d+)/, Pattern.CASE_INSENSITIVE).matcher(d)
			if (matcher.find()) return [matcher.group(1).toDouble(), matcher.group(2).toDouble()]
		}
		return null
	}
}
