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
import org.openscience.cdk.isomorphism.Pattern

def cli = new CliBuilder(usage: 'groovy Main.groovy [options]')
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

def substitutions = [
	[name: "thiobarbiturate",         smiles: "O=C1NC(=S)NC(=O)C1", rot: 60, flipx: true],
	[name: "barbiturate",             smiles: "C1(C(=O)NC(=O)NC1=O)", rot: -60, flipx: true],

	[name: "β-carboline",             smiles: "C1=CC=C2C(=C1)C3=C(N2)C=NC=C3", bany: true],
	[name: "hexahydroazepinoindole",  smiles: "C1CNCCC2=C1C3=CC=CC=C3N2", rot: -18, bany: true, flipx: true, flipy: true],
	[name: "α-alkyltryptamine",       smiles: "CC(CC1=CNC2=CC=CC=C21)N", its: new String[] { "tryptamine" }, bany: true, amph: true],
	[name: "β-alkyltryptamine",       smiles: "CC(CN)C1=CNC2=CC=CC=C21", its: new String[] { "tryptamine" }, bany: true],
	[name: "tryptamine",              smiles: "C1=CC=C2C(=C1)C(=CN2)CCN", rot: -42, bany: true],

	[name: "piperidinophenone",       smiles: "CC(C(=O)C1=CC=CC=C1)N2CCCCC2", its: new String[] { "phenethylamine" }, amph: true],
	[name: "pyrrolidinophenone",      smiles: "CC(C(=O)C1=CC=CC=C1)N2CCCC2", its: new String[] { "phenethylamine" }, amph: true],
	[name: "cathinone",               smiles: "CC(C(=O)C1=CC=CC=C1)N", its: new String[] { "phenethylamine" }, amph: true],
	[name: "phentermine",             smiles: "CC(C)(CC1=CC=CC=C1)N", its: new String[] { "phenethylamine" } ],
	[name: "amphetamine",             smiles: "CC(CC1=CC=CC=C1)N", its: new String[] { "phenethylamine" }, amph: true],
	[name: "phenethylamine",          smiles: "C1=CC=C(C=C1)CCN", flipy: true],

	[name: "benzylpiperazine",        smiles: "C1CN(CCN1)CC2=CC=CC=C2", its: new String[] { "benzylamine" }],
	[name: "benzylamine",             smiles: "C1=CC=C(C=C1)CN", flipy: true],

	[name: "phenylpiperazine",        smiles: "C1CN(CCN1)C2=CC=CC=C2", its: new String[] { "aniline" }],
	[name: "aniline",                 smiles: "C1=CC=C(C=C1)N", flipx: true, flipy: true],
]

for (sub in substitutions) {
	sub.mol = sp.parseSmiles(sub.smiles)
	bm = BondMatcher.forOrder()
	if (sub.bany)
		bm = BondMatcher.forAny()

	sub.pattern = VentoFoggia.findSubstructure(sub.mol, AtomMatcher.forElement(), bm)
}

for (sub in substitutions) {
	if (sub.pattern.match(target).length > 0) {
		if (sub.its) {
			IAtomContainer lsubst = null
			Pattern lpattern = null
			for (it in sub.its) {
				for (ssub in substitutions) {
					if (it == ssub.name || it == ssub.fame) {
						if (lsubst == null)
							sdg.generateCoordinates(ssub.mol)
						else
							sdg.generateAlignedCoordinates(ssub.mol, lsubst, lpattern)
						if (ssub.rot)
							Align.rotate(ssub.mol, ssub.rot)

						if (ssub.flipx)
							Align.flipx(ssub.mol)
						if (ssub.flipy)
							Align.flipy(ssub.mol)

						lsubst = ssub.mol
						lpattern = ssub.pattern
						break
					}
				}
			}
			if (lsubst == null)
				sdg.generateCoordinates(sub.mol)
			else
				sdg.generateAlignedCoordinates(sub.mol, lsubst, lpattern)
		} else {
			sdg.generateCoordinates(sub.mol)
		}
		if (sub.rot)
			Align.rotate(sub.mol, sub.rot)

		if (sub.flipx)
			Align.flipx(sub.mol)
		if (sub.flipy)
			Align.flipy(sub.mol)

		sdg.generateAlignedCoordinates(target, sub.mol, sub.pattern);

		if (sub.amph)
			Align.amphetamine_fix(target)

		if (sub.name) {
			println "Class: ${sub.name}"
			break
		}
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
