//@Grab(group='org.openscience.cdk', module='cdk-bundle', version='2.11')
//@Grab(group='org.openscience.cdk', module='cdk-depict', version='2.11')

import java.util.ArrayList
import java.awt.Font

import groovy.lang.GroovyObject
import groovy.cli.commons.CliBuilder
import groovy.json.JsonOutput

import org.openscience.cdk.depict.DepictionGenerator
import org.openscience.cdk.renderer.generators.standard.StandardGenerator
import org.openscience.cdk.smiles.SmilesParser
import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.layout.StructureDiagramGenerator

import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IBond
import org.openscience.cdk.interfaces.IAtomContainer

import org.openscience.cdk.isomorphism.VentoFoggia
import org.openscience.cdk.isomorphism.AtomMatcher
import org.openscience.cdk.isomorphism.BondMatcher
import org.openscience.cdk.isomorphism.Pattern

class Main {
static void main(String [] args) {
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
if (!options) {
	println "failed to parse options"
	return
}
if (!options.m && !options.r) {
	println "no molecule or reaction specified"
	return
}
if (options.m && options.r) {
	println "both molecule and reaction specified"
	return
}
if (options.s && options.r) {
	println "both salt and reaction specified"
	return
}

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

def target = null
def salt = null

def svg = ""

if (options.m) target = sp.parseSmiles(options.m)
if (options.s) salt = sp.parseSmiles(options.s)
if (options.r) target = sp.parseReactionSmiles(options.r)

def sdg = new StructureDiagramGenerator()

def substitutions = [
	[lame: "3,4-methylenedioxyphenethylamine", smiles: "C1OC2=C(O1)C=C(C=C2)CCN"],
	[lame: "cathinone",                        smiles: "CC(C(=O)C1=CC=CC=C1)N"],

	[name: "thiobarbiturate",                  smiles: "O=C1NC(=S)NC(=O)C1", rot: 60, flipx: true],
	[name: "barbiturate",                      smiles: "C1(C(=O)NC(=O)NC1=O)", rot: -60, flipx: true],

	[name: "β-carboline",                      smiles: "C1=CC=C2C(=C1)C3=C(N2)C=NC=C3", bany: true],
	[name: "hexahydroazepinoindole",           smiles: "C1CNCCC2=C1C3=CC=CC=C3N2", rot: -18, bany: true, flipx: true, flipy: true],
	[name: "α-alkyltryptamine",                smiles: "CC(CC1=CNC2=CC=CC=C21)N", its: new String[] { "tryptamine" }, bany: true, amph: true],
	[name: "β-alkyltryptamine",                smiles: "CC(CN)C1=CNC2=CC=CC=C21", its: new String[] { "tryptamine" }, bany: true],
	[name: "tryptamine",                       smiles: "C1=CC=C2C(=C1)C(=CN2)CCN", rot: -42, bany: true],


	[name: "naphthylaminopropane",             smiles: "CC(CC1=CC2=CC=CC=C2C=C1)N", amph: true],

	[fame: "phen",                             smiles: "OCC(C1CCCCN1)C2=CC=CC=C2", its: new String[] { "2-benzylpiperidine" }, amph: false, nself: true],
	[fame: "naphphen",                         smiles: "COCC(C1CCCCN1)C2=CC3=CC=CC=C3C=C2", its: new String[] { "2-benzylpiperidine" }, amph: false, nself: true],
	[name: "phenidate",                        smiles: "OC(=O)C(C1CCCCN1)C2=CC3=CC=CC=C3C=C2", its: new String[] { "2-benzylpiperidine", "naphphen"}, amph: false, nself: true],
	[name: "phenidate",                        smiles: "OC(=O)C(C1CCCCN1)C2=CC=CC=C2", its: new String[] { "2-benzylpiperidine", "phen"}, amph: false, nself: true],

	[name: "2-benzylpyrrolidine",              smiles: "C1CNC(C1)CC2=CC=CC=C2", its: new String[] { "phenethylamine", "amphetamine" }, amph: false],
	[name: "2-benzylpiperidine",               smiles: "C1CCNC(C1)CC2=CC=CC=C2", its: new String[] { "phenethylamine", "amphetamine" }, amph: false, flipy: true],
	[name: "2-benzylpiperazine",               smiles: "N1CCNC(C1)CC2=CC=CC=C2", its: new String[] { "phenethylamine", "amphetamine" }, amph: false, flipy: true],
	[name: "3-benzylmorpholine",               smiles: "C1COCC(N1)CC2=CC=CC=C2", its: new String[] { "phenethylamine", "amphetamine" }, amph: false, flipx: true],

	[fame: "methylphenethylamine",             smiles: "C1=CC=C(C=C1)CCNC", flipy: true, bany: false],
	[fame: "methamphetamine",                  smiles: "CC(CC1=CC=CC=C1)NC"],
	[fame: "sphenethylamine",                  smiles: "C(CC1=CC=CC=C1)N", bany: true, flipy: true],

	[name: "1,2-diarylethylamine",             smiles: "C1=CC=C(C=C1)CC(C2=CC=CC=C2)NC" , its: new String[] { "phenethylamine", "methamphetamine" }, amph: false, nself: true],
	[name: "1,2-diarylethylamine",             smiles: "C1=CC=C(C=C1)CC(C2=CC=CC=C2)N" , its: new String[] { "phenethylamine" }, amph: false],

	[name: "piperidinophenone",                smiles: "CC(C(=O)C1=CC=CC=C1)N2CCCCC2", its: new String[] { "phenethylamine" }, amph: true],
	[name: "pyrrolidinophenone",               smiles: "CC(C(=O)C1=CC=CC=C1)N2CCCC2", its: new String[] { "phenethylamine" }, amph: true],

	[fame: "metamph",                          smiles: "CC=(CCCN)C"],
	[fame: "oramph",                           smiles: "C=C(CCN)C"],

	[name: "cathinone",                        smiles: "CC1=CC=CC=C1C(=O)C(C)NC", flipx: true, its: new String[] { "oramph", "methylphenethylamine" }, amph: true],
	[name: "cathinone",                        smiles: "CC1=CC(=CC=C1)C(=O)C(C)NC", its: new String[] { "metamph" }, amph: true],

	[name: "cathinone",                        smiles: "CC1=CC=CC=C1C(=O)C(C)N", flipx: true, its: new String[] { "oramph", "phenethylamine" }, amph: true],
	[name: "cathinone",                        smiles: "CC1=CC(=CC=C1)C(=O)C(C)N", its: new String[] { "metamph", "sphenethylamine"}, amph: true],

	[name: "amphetamine",                        smiles: "CC1=CC=CC=C1CC(C)N", flipx: true, its: new String[] { "oramph", "phenethylamine" }, amph: true],
	[name: "amphetamine",                        smiles: "CC1=CC(=CC=C1)CC(C)N", its: new String[] { "metamph", "sphenethylamine"}, amph: true],

	[name: "cathinone",                        smiles: "CC(C(=O)C1=CC=CC=C1)NC", its: new String[] { "methylphenethylamine" }, amph: true],
	[name: "cathinone",                        smiles: "CC(C(=O)C1=CC=CC=C1)N", its: new String[] { "phenethylamine" }, amph: true],

	[fame: "erp",                              smiles: "C-C-C-N"],
	[fame: "tsph",                             smiles: "C(CC1=CC=CS1)N"],
	[name: "thiopropamine",                    smiles: "CC(CC1=CC=CS1)N", its: new String[] { "erp", "tsph" }, flipx: true, amph: true],

	[name: "1-aminoindane",                    smiles: "C1CC2=CC=CC=C2C1N", flipx: true],
	[name: "1-aminoindane",                    smiles: "C1=CC2=C(CCC2N)C=C1"],
	[name: "2-aminoindane",                    smiles: "C1C(CC2=CC=CC=C21)N", bany: true ],

	//[name: "phenylethanolamine",               smiles: "CC(CC1=CC=CC=C1)N", its: new String[] { "phenethylamine" }, amph: true],
	[name: "phentermine",                      smiles: "CC(C)(CC1=CC=CC=C1)N", its: new String[] { "phenethylamine" } ],

	[name: "amphetamine",                      smiles: "CC(CC1=CC=CC=C1)N", its: new String[] { "phenethylamine" }, amph: true],

	[name: "phenylethanolamine",               smiles: "C1=CC=C(C=C1)C(CN)O", its: new String[] { "phenethylamine" }],

	[name: "phenylpropylamine",                smiles: "C1=CC=C(C=C1)CCCN", flipy: true],
	[name: "phenethylamine",                   smiles: "C1=CC=C(C=C1)CCN", flipy: true],

	[fame: "pipcrp",                           smiles: "C1CCC(CC1)(C)N3CCCCC3", rot: 100, bany: true],
	[name: "arylcyclohexylpiperidine",         smiles: "C1CCC(CC1)(C2=CC=CC=C2)N3CCCCC3", its: new String[] { "pipcrp" }, nself: true, flipy: true],

	[fame: "mopcrp",                           smiles: "C1CCC(CC1)(C)N3CCOCC3", rot: 100, bany: true],
	[name: "arylcyclohexylmorpholine",         smiles: "C1CCC(CC1)(C2=CC=CC=C2)N3CCOCC3", its: new String[] { "mopcrp" }, nself: true, flipy: true],


	[fame: "pyrcrp",                           smiles: "C1CCC(CC1)(C)N3CCCC3", rot: 100, bany: true, flipx: true],
	[name: "arylcyclohexylpyrrolidine",        smiles: "C1CCC(CC1)(C2=CC=CC=C2)N3CCCC3", its: new String[] { "pyrcrp" }, nself: true],

	[fame: "oxcrp",                            smiles: "O=C1CCCCC1(N)C", rot: 60, bany: true],
	[name: "arylcyclohexylamine",              smiles: "NC1(CCCCC1=O)C2=CC=CC=C2", its: new String[] { "oxcrp" }, flipx: true, flipy: true, bany: true, nself: true],

	[fame: "crp",                              smiles: "C1CCCCC1(N)C", rot: 60, bany: true],
	[name: "arylcyclohexylamine",              smiles: "C1CCC(CC1)(C2=CC=CC=C2)N", its: new String[] { "crp" }, bany: true, nself: true],

	[name: "1-benzylpiperazine",               smiles: "C1CN(CCN1)CC2=CC=CC=C2", its: new String[] { "benzylamine" }],
	[name: "benzylamine",                      smiles: "C1=CC=C(C=C1)CN", flipy: true],

	[name: "phenylpiperazine",                 smiles: "C1CN(CCN1)C2=CC=CC=C2", its: new String[] { "aniline" }],
	[name: "aniline",                          smiles: "C1=CC=C(C=C1)N", flipx: true, flipy: true],

	[name: "cyclohexylamine",                  smiles: "C1CCC(CC1)N", flipx: true, flipy: true],

	[name: "phenol",                           smiles: "C1=CC=C(C=C1)O", rot: 60],
]

def classes = []

for (sub in substitutions) {
	sub.mol = sp.parseSmiles(sub.smiles)
	def bm = BondMatcher.forOrder()
	if (sub.bany)
		bm = BondMatcher.forAny()

	sub.pattern = VentoFoggia.findSubstructure(sub.mol, AtomMatcher.forElement(), bm)
}

for (sub in substitutions) {
	if (sub.pattern.match(target).length > 0 && sub.fame == null) {
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
						if (ssub.rot != null)
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
			if (lsubst == null) {
				if (sub.nself == true)
					sdg.generateCoordinates(target)
				else
					sdg.generateCoordinates(sub.mol)
			} else {
				if (sub.nself == true)
					sdg.generateAlignedCoordinates(target, lsubst, lpattern)
				else
					sdg.generateAlignedCoordinates(sub.mol, lsubst, lpattern)
			}
		} else {
			if (sub.lame == null) {
				if (sub.nself == true)
					sdg.generateCoordinates(target)
				else
					sdg.generateCoordinates(sub.mol)
			}
		}
		if (sub.lame == null) {
			if (sub.nself == true) {
				if (sub.rot != null)
					Align.rotate(target, sub.rot)
				if (sub.flipx == true)
					Align.flipx(target)
				if (sub.flipy == true)
					Align.flipy(target)
			} else {
				if (sub.rot != null)
					Align.rotate(sub.mol, sub.rot)
				if (sub.flipx == true)
					Align.flipx(sub.mol)
				if (sub.flipy == true)
					Align.flipy(sub.mol)

				sdg.generateAlignedCoordinates(target, sub.mol, sub.pattern);
			}

			if (sub.amph)
				Align.amphetamine_fix(target)
		}

		if (sub.name) {
			classes.add(sub.name)
			break
		}
		if (sub.lame) {
			classes.add(sub.lame)
		}
	}
}

classes.unique()

if (classes.size() > 0)
	println "Classes: ${classes}"

def depict = new DepictionGenerator(new Font("monospace", Font.PLAIN, 18))
	.withAtomColors()
	.withZoom(3.0)
	.withPadding(13.5)
	.withParam(StandardGenerator.StrokeRatio.class, 1.4d)

if (options.m && options.s) {
	def molecules = new ArrayList<IAtomContainer>()
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

svg = Highlight.modifySVG(svg).replaceAll(/>\s+</, "><")

new File(options.o ?: "molecule.svg").withWriter("UTF-8") { writer ->
	writer.write(svg)
}

println "Rendered to ${options.o ?: 'molecule.svg'}"

def json_data = [
	svg: svg,
	classes: classes
]
def json = JsonOutput.prettyPrint(JsonOutput.toJson(json_data))

new File(options.d ?: "molecule.json").withWriter("UTF-8") { writer ->
	writer.write(json)
}

}
}
