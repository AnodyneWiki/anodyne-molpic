import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IAtomContainer

class Align {
	static void flipx(IAtomContainer mol) {

	}

	static void flipy(IAtomContainer mol) {
		for (IAtom atom : mol.atoms()) {
			atom.getPoint2d().x = -atom.getPoint2d().x;
		}
	}
}
