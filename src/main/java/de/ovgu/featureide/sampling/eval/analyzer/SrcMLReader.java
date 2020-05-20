package de.ovgu.featureide.sampling.eval.analyzer;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.prop4j.And;
import org.prop4j.ErrorLiteral;
import org.prop4j.Literal;
import org.prop4j.NodeReader;
import org.prop4j.Not;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SrcMLReader {

	private static final Pattern ifPattern = Pattern.compile("#\\s*if(\\(|\\s)\\s*(.+)", Pattern.DOTALL);
	private static final Pattern ifDefPattern = Pattern.compile("\\A#\\s*ifdef\\s+(\\w+)");
	private static final Pattern ifNDefPattern = Pattern.compile("\\A#\\s*ifndef\\s+(\\w+)");
	private static final Pattern definePattern = Pattern.compile("\\A#\\s*define\\s+(\\w+)");
	private static final Pattern elifPattern = Pattern.compile("#\\s*elif(\\(|\\s)\\s*(.+)", Pattern.DOTALL);
	private static final Pattern definedPattern = Pattern.compile("(\\!\\s*)?defined(\\s+\\w+|\\s*\\(\\s*\\w+\\s*\\))");

	private final ArrayDeque<org.prop4j.Node> ifStack = new ArrayDeque<>();

	protected Stream.Builder<RawPresenceCondition> expressions;
	protected NodeReader nodeReader;
	protected Path file = null;

	public static class DefineFinder extends SrcMLReader {

		protected boolean parseChildren(final Node node, int depth) {
			final NodeList childNodes = node.getChildNodes();

			for (int i = 0; i < childNodes.getLength(); i++) {
				final Node child = childNodes.item(i);
				switch (child.getNodeName()) {
				case "cpp:define":
					Matcher matcher = definePattern.matcher(child.getTextContent());
					if (matcher.find()) {
						final String featureName = matcher.group(1);
						expressions.add(new RawPresenceCondition(featureName, file));
					} else {
						throw new RuntimeException(child.getTextContent());
					}
					break;
				default:
					break;
				}
				parseChildren(child, depth + 1);
			}
			return true;
		}
	}

	public Stream<RawPresenceCondition> readExpressions(NodeReader nodeReader, Path file) {
		try {
			final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final Element documentElement = documentBuilder.parse(file.toFile()).getDocumentElement();
			documentElement.normalize();

			this.file = file;

			ifStack.clear();
			expressions = Stream.builder();
			this.nodeReader = nodeReader;
			parseChildren(documentElement, 0);
			return expressions.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Stream.empty();
		}
	}

	protected boolean parseChildren(final Node node, int depth) {
		final NodeList childNodes = node.getChildNodes();
		int numNodes = childNodes.getLength();

		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node child = childNodes.item(i);
			switch (child.getNodeName()) {
			case "cpp:define":
			case "cpp:undef":
			case "cpp:include":
			case "cpp:pragma":
			case "cpp:warning":
			case "cpp:error":
			case "cpp:line":
			case "cpp:empty":
			case "cpp:directive":
			case "cpp:macro":
			case "cpp:value":
			case "cpp:number":
			case "cpp:expr":
			case "cpp:file":
				break;
			case "cpp:if":
				addIf(parseIf(child, false));
				break;
			case "cpp:ifdef": {
				Matcher matcher = ifDefPattern.matcher(child.getTextContent());
				if (matcher.find()) {
					final String featureName = matcher.group(1);
					addIf(new Literal(featureName));
				} else {
					throw new RuntimeException(child.getTextContent());
				}
				break;
			}
			case "cpp:ifndef": {
				Matcher matcher = ifNDefPattern.matcher(child.getTextContent());
				if (matcher.find()) {
					final String featureName = matcher.group(1);
					addIf(new Literal(featureName, false));
				} else {
					throw new RuntimeException(child.getTextContent());
				}
				break;
			}
			case "cpp:else": {
				addIf(new Not(ifStack.removeLast()));
				break;
			}
			case "cpp:elif":
				addIf(new And(new Not(ifStack.removeLast()), parseIf(child, true)));
				break;
			case "cpp:endif":
				removeIf();
				break;
			default:
				numNodes--;
				break;
			}
			parseChildren(child, depth + 1);
		}
		return numNodes > 0;
	}

	private void addIf(org.prop4j.Node node) {
		ifStack.addLast(node);
		addExpression();
	}

	private void removeIf() {
		if (ifStack.isEmpty()) {
			System.out.println("WARNING -- empty ifdef stack");
			return;
		}
		ifStack.removeLast();
		addExpression();
	}

	private void addExpression() {
		if (!ifStack.isEmpty()) {
			And and = new And(ifStack);
			expressions.add(new RawPresenceCondition(and.toString(), file));
		}
	}

	private org.prop4j.Node parseIf(Node ifNode, boolean elif) {
		String textContent = ifNode.getTextContent();
		if (textContent != null) {
			textContent = textContent.replace("\\\n", "").trim();
			final Matcher matcher = (elif ? elifPattern : ifPattern).matcher(textContent);
			if (matcher.matches()) {
				String expression = matcher.group(2).trim();
				final Matcher defineMatcher = definedPattern.matcher(expression);
				if (defineMatcher.find()) {
					// removes the defined keyword (more precise, retains only "!" and the feature name)
					expression = defineMatcher.replaceAll("$1 $2");
				}
				return nodeReader.stringToNode(expression);
			}
		}
		return new ErrorLiteral(textContent);
	}

}
