import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IBond
import org.openscience.cdk.interfaces.IStereoElement
import org.openscience.cdk.interfaces.IAtomContainer

class Align {
	static void flipx(IAtomContainer mol) {

	}

	static void flipy(IAtomContainer mol) {
		for (IAtom atom : mol.atoms()) {
			atom.getPoint2d().x = -atom.getPoint2d().x;
		}
	}

	static void amphetamine_fix(IAtomContainer molecule) {
		println("amphetamine fix running!")
		for (IStereoElement<?, ?> se : molecule.stereoElements()) {
			if (se.getConfigClass() == IStereoElement.Tetrahedral) {
				IBond.Display disp = IBond.Display.Solid
				IBond.Stereo ster = IBond.Stereo.NONE
				ArrayList<IBond> bondi = new ArrayList<IBond>()
				for (IBond bond : ((IAtom) se.getFocus()).bonds()) {
					IAtom targ = null
					if (bond.getBegin().getSymbol() == "C" && bond.getEnd().getSymbol() == "N" ) {
						targ = bond.getBegin()
					} else if (bond.getBegin().getSymbol() == "N" && bond.getEnd().getSymbol() == "C" ) {
						targ = bond.getEnd()
					} else {
						continue
					}

					for (IBond cbond : targ.bonds()) {
						if (cbond.getBegin().getSymbol() != "C" || cbond.getEnd().getSymbol() != "C") continue
						if (cbond.getStereo() != IBond.Stereo.NONE || cbond.getDisplay() != IBond.Display.Solid) continue
						bondi.add(cbond)
					}
					for (IBond cbond : targ.bonds()) {
						if (cbond.getStereo() == IBond.Stereo.NONE || cbond.getDisplay() == IBond.Display.Solid) continue
						
						ster = bond.getStereo()
						disp = bond.getDisplay()
						bond.setStereo(IBond.Stereo.NONE)
						bond.setDisplay(IBond.Display.Solid)
					}
					if (bondi.size() > 0) {
						break
					}
				}
				for (IBond bond : bondi) {
					if (bond.getBegin().getBondCount() != 1 && bond.getEnd().getBondCount() != 1) {
						continue
					}
					println("count: " + bond.getBegin().getBondCount())
					println("count end: " + bond.getEnd().getBondCount())
					if (bond.getBegin().getBondCount() < bond.getEnd().getBondCount()) {
						println("inverting!")
						if (disp == IBond.Display.WedgedHashBegin)
							disp = IBond.Display.WedgedHashEnd
						if (disp == IBond.Display.WedgeBegin)
							disp = IBond.Display.WedgeEnd
						if (ster == IBond.Stereo.DOWN)
							ster = IBond.Stereo.DOWN_INVERTED
						if (ster == IBond.Stereo.UP)
							ster = IBond.Stereo.UP_INVERTED
					}
					println(ster)
					println(disp)
					println(bond.getStereo().toString())
					println(bond.getDisplay().toString())
					bond.setStereo(ster)
					bond.setDisplay(disp)
				}
			}
		}
	}
}
