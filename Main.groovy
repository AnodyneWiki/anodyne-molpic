@Grab(group='org.openscience.cdk', module='cdk-bundle', version='2.11')
@Grab(group='org.openscience.cdk', module='cdk-depict', version='2.11')

import java.util.ArrayList
import java.awt.Font
import groovy.cli.commons.CliBuilder

import org.openscience.cdk.depict.DepictionGenerator
import org.openscience.cdk.renderer.generators.standard.StandardGenerator
import org.openscience.cdk.smiles.SmilesParser
import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.layout.StructureDiagramGenerator

import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IAtomContainer

import org.openscience.cdk.isomorphism.VentoFoggia
import org.openscience.cdk.isomorphism.AtomMatcher
import org.openscience.cdk.isomorphism.BondMatcher

def cli = new CliBuilder(usage: 'groovy src/main.groovy [options]')
cli.m(longOpt: 'molecule', args: 1, 'Molecule SMILES')
cli.s(longOpt: 'salt', args: 1, 'Salt SMILES')
cli.am(longOpt: 'amine-count', args: 1, 'Amine count')
cli.ac(longOpt: 'acid-count', args: 1, 'Salt count')
cli.r(longOpt: 'reaction', args: 1, 'Reaction SMILES')
cli.o(longOpt: 'output', args: 1, 'Output file')
cli.d(longOpt: 'debug-file', args: 1, 'Debug file')
cli.v(longOpt: 'verbose', 'Enable verbose mode')

def options = cli.parse(args)
if (!options) return
if (!options.m && !options.r) return
if (options.m && options.r) return
if (options.s && options.r) return

def amine_count = options.am ? options.am.toInteger() : 1
def acid_count = options.ac ? options.ac.toInteger() : 1

if (options.v) {
	if (options.m) println "Molecule: ${options.m}"
	if (options.s) println "Salt: ${options.s}"
	if (amine_count) println "Salt Amine Count: ${amine_count}"
	if (acid_count) println "Salt Acid Count: ${acid_count}"
	if (options.r) println "Reaction: ${options.r}"
	println "Output: ${options.o ?: 'molecule.svg'}"
	// println "Verbose: ${options.v}"
}

def builder = DefaultChemObjectBuilder.instance
def sp = new SmilesParser(builder)

target = null
salt = null

svg = ""

if (options.m) target = sp.parseSmiles(options.m)
if (options.s) salt = sp.parseSmiles(options.s)
if (options.r) target = sp.parseReactionSmiles(options.r)

def sdg = new StructureDiagramGenerator()

def alignSubstitution(StructureDiagramGenerator sdg, SmilesParser sp, IAtomContainer target, String smiles, String name) {
	substitution_structure = sp.parseSmiles(smiles)
	substitution_pattern = VentoFoggia.findSubstructure(substitution_structure, AtomMatcher.forElement(), BondMatcher.forOrder())

	if (substitution_pattern.match(target).length > 0) {
		sdg.generateAlignedCoordinates(target, substitution_structure, substitution_pattern);
		Align.flipy(target)
		return true
	}
	return false
}

def substitutions = [
	[smiles: "NCCc1ccccc1", name: "phenethylamine"],
]

for (sub in substitutions) {
	def result = alignSubstitution(sdg, sp, target, sub.smiles, sub.name)

	if (result) {
		println "Class: ${sub.name}"
		break
	}
}

def depict = new DepictionGenerator(new Font("monospace", Font.PLAIN, 18))
	.withAtomColors()
	.withZoom(3.0)
	.withPadding(13.5)
	.withParam(StandardGenerator.StrokeRatio.class, 1.4d)

if (options.m && options.s) {
	molecules = new ArrayList<IAtomContainer>()
	for (int ami = 0; ami < amine_count; ami++) {
		molecules.add(target)
	}
	for (int cmi = 0; cmi < acid_count; cmi++) {
		molecules.add(salt)
	}
	svg = depict.depict(molecules).toSvgStr()
} else {
	svg = depict.depict(target).toSvgStr()
}

if (!svg) return

svg = Highlight.modifySVG(svg)

new File(options.o ?: "molecule.svg").withWriter("UTF-8") { writer ->
	writer.write(svg)
}

println "Rendered to ${options.o ?: 'molecule.svg'}"

new File(options.d ?: "molecule.json").withWriter("UTF-8") { writer ->
	writer.write(svg)
}
