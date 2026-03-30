import javax.vecmath.Point2d

import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IBond
import org.openscience.cdk.interfaces.IStereoElement
import org.openscience.cdk.interfaces.IAtomContainer

import org.openscience.cdk.geometry.GeometryUtil

class Align {
	static void flip(IBond bond) {
		switch (bond.getDisplay()) {
			case IBond.Display.WedgeBegin:
				bond.setDisplay(IBond.Display.WedgedHashBegin)
				break
			case IBond.Display.WedgeEnd:
				bond.setDisplay(IBond.Display.WedgedHashEnd)
				break
			case IBond.Display.WedgedHashBegin:
				bond.setDisplay(IBond.Display.WedgeBegin)
				break
			case IBond.Display.WedgedHashEnd:
				bond.setDisplay(IBond.Display.WedgeEnd)
				break
			case IBond.Display.Bold:
				bond.setDisplay(IBond.Display.Hash)
				break
			case IBond.Display.Hash:
				bond.setDisplay(IBond.Display.Bold)
				break
		}
		switch (bond.getStereo()) {
			case IBond.Stereo.UP:
				bond.setStereo(IBond.Stereo.DOWN)
				break
			case IBond.Stereo.UP_INVERTED:
				bond.setStereo(IBond.Stereo.DOWN_INVERTED)
				break
			case IBond.Stereo.DOWN:
				bond.setStereo(IBond.Stereo.UP)
				break
			case IBond.Stereo.DOWN_INVERTED:
				bond.setStereo(IBond.Stereo.UP_INVERTED)
				break
			case IBond.Stereo.NONE:
			default:
				break;
		}
	}

	static void flipx(IAtomContainer mol) {
		for (IAtom atom : mol.atoms()) {
			atom.getPoint2d().y = -atom.getPoint2d().y
		}
	}

	static void flipy(IAtomContainer mol) {
		for (IAtom atom : mol.atoms()) {
			atom.getPoint2d().x = -atom.getPoint2d().x;
		}
	}

	static void rotate(IAtomContainer mol, int degree) {
		Point2d center = GeometryUtil.get2DCenter(mol)
		GeometryUtil.rotate(mol, center, Math.toRadians(degree))
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
