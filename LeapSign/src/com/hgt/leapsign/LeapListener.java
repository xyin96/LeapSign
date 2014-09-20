package com.hgt.leapsign;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Vector;

public class LeapListener extends Listener {
	private Hand hand;
	private String str, filter;
	private StringBuilder word;
	private int fcount = 0;

	public void onConnect(Controller controller) {
		System.out.println("Connected");

	}

	public void onFrame(Controller controller) {
		if (word == null) {
			word = new StringBuilder();
		}
		// System.out.println("Frame available");
		Frame frame = controller.frame();
		hand = frame.hands().rightmost();
		// Average a finger position for the last 10 frames
		int count = 0;
		ArrayList<Vector> fingerDir = new ArrayList<Vector>();
		ArrayList<Vector> fingerLoc = new ArrayList<Vector>();
		for (Finger fingerToAverage : frame.fingers()) {
			Vector averageDir = new Vector();
			Vector averageLoc = new Vector();
			for (int i = 0; i < 10; i++) {
				Finger fingerFromFrame = controller.frame(i).finger(
						fingerToAverage.id());
				if (fingerFromFrame.isValid()) {
					averageLoc = averageLoc.plus(fingerFromFrame.tipPosition());
					averageDir = averageDir.plus(fingerFromFrame.direction());
					count++;
				}
				averageDir = averageDir.divide(count);
				averageLoc = averageLoc.divide(count);
			}
			// System.out.println(fingerToAverage.type() + "_DIR: " +
			// averageDir);
			// System.out.println(fingerToAverage.type() + "_LOC: " +
			// averageLoc);
			fingerDir.add(averageLoc);
			fingerLoc.add(averageLoc);
		}

		String str1 = analyze(fingerDir, fingerLoc);
		if (fcount > 45) {
			if (str1 == null && word.length() > 0) {
				filter = word.toString();
				System.out.println("filter: " + filter);

				URL t2v = null;

				try {
					t2v = new URL(
							"http://translate.google.com/translate_tts?ie=UTF-8&tl=ja&q"
									+ URLEncoder.encode(filter));

				} catch (Exception e) {
				}
				;

				LeapSign.jtf2.setText(filter);
				word = new StringBuilder();
			} else if (str1 == null) {
			} else if (str1.equals(str) || fcount < 15) {

			} else {
				System.out.println(str1);
				str = str1;
				LeapSign.jtf.setText(str);
				word.append(str);

			}
			fcount = 0;
		} else if (str1 != str) {
			fcount++;
		}

	}

	public String analyze(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		double fVel = 0.0;
		for (Finger f : hand.fingers()) {
			fVel += f.tipVelocity().magnitude();
		}
		//System.out.println(fVel + "");
		if (Math.sqrt(fVel) < 30) {
			if (!hand.fingers().fingerType(Finger.Type.TYPE_INDEX).get(0)
					.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_PINKY)
							.get(0).isExtended()
					&& hand.palmNormal().getY() < -0.75) {
				return aTest(fingerDirs, fingerLoc);
			} else if (hand.palmNormal().getY() < -0.75
					&& hand.fingers().fingerType(Finger.Type.TYPE_INDEX).get(0)
							.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_PINKY).get(0)
							.isExtended()) {
				return bTest(fingerDirs, fingerLoc);
			} else if (hand.palmNormal().getY() < -0.75
					&& !hand.fingers().fingerType(Finger.Type.TYPE_INDEX)
							.get(0).isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_PINKY).get(0)
							.isExtended()) {
				return fTest(fingerDirs, fingerLoc);
			} else if (hand.palmNormal().getX() < -0.75) {
				return cTest(fingerDirs, fingerLoc);
			} else if (hand.fingers().fingerType(Finger.Type.TYPE_INDEX).get(0)
					.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_PINKY)
							.get(0).isExtended()
					&& hand.palmNormal().getY() < -0.75) {
				return dTest(fingerDirs, fingerLoc);
			} else if (hand.fingers().fingerType(Finger.Type.TYPE_INDEX).get(0)
					.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_PINKY)
							.get(0).isExtended()
					&& hand.palmNormal().getY() < -0.75) {
				return hTest(fingerDirs, fingerLoc);
			} else if (hand.fingers().fingerType(Finger.Type.TYPE_THUMB).get(0)
					.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_INDEX)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_PINKY).get(0)
							.isExtended() && hand.palmNormal().getY() < -0.75) {
				return yTest(fingerDirs, fingerLoc);
			} else if (!hand.fingers().fingerType(Finger.Type.TYPE_INDEX)
					.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_PINKY).get(0)
							.isExtended() && hand.palmNormal().getY() < -0.75) {
				return iTest(fingerDirs, fingerLoc);
			} else if (hand.fingers().fingerType(Finger.Type.TYPE_INDEX).get(0)
					.isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_PINKY)
							.get(0).isExtended()
					&& hand.palmNormal().getY() < -0.75) {
				return wTest(fingerDirs, fingerLoc);
			} else if (!hand.fingers().fingerType(Finger.Type.TYPE_INDEX)
					.get(0).isExtended()
					&& hand.fingers().fingerType(Finger.Type.TYPE_MIDDLE)
							.get(0).isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_RING).get(0)
							.isExtended()
					&& !hand.fingers().fingerType(Finger.Type.TYPE_PINKY)
							.get(0).isExtended()
					&& hand.palmNormal().getY() < -0.75) {
				return "FUCK YOU";
			} else {
				return miscTest(fingerDirs, fingerLoc);
			}
		}
		return null;
	}

	private String aTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		// System.out.println(thumb);
		// System.out.println(thumb.cross(fingerDirs.get(1)));
		if (thumb.getX() < -1.5) {
			return "A";
		} else if (thumb.getX() < 1) {
			return "E";
		}
		return null;
	}

	private String miscTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		if (Math.abs(hand.palmNormal().getZ() - 1) < 0.1) {
			if (fingerDirs.get(1).cross(fingerDirs.get(2)).magnitude() > 10) {
				return "P";
			} else {
				return "Q";
			}
		}

		return null;
	}

	private String bTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		Vector thumbLoc = fingerLoc.get(0);

		if (fingerLoc.get(1).getX() < thumbLoc.getX()
				&& thumbLoc.getX() < fingerLoc.get(4).getX()) {
			return "B";
		}
		return null;
	}

	private String cTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		if (fingerLoc.get(0).distanceTo(fingerLoc.get(2)) < 15) {
			return "O";
		} else if (fingerLoc.get(0).distanceTo(fingerLoc.get(2)) > 20) {
			return "C";
		}
		return null;
	}

	private String dTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		// System.out.println(hand.finger(1).isExtended());
		// System.out.println(fingerLoc.get(1));
		// System.out.println(fingerDirs.get(1).getX());
		if (thumb.cross(fingerDirs.get(1)).magnitude() > 100) {
			return "L";
		} else if (fingerLoc.get(1).getX() < fingerLoc.get(2).getX()) {
			return "D";
		} else if (fingerLoc.get(1).getX() > fingerLoc.get(2).getX()) {
			return "G";
		}
		return null;
	}

	private String fTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		// System.out.println(hand.finger(1).isExtended());
		// System.out.println(thumb.cross(fingerDirs.get(1)));

		return "F";
	}

	private String yTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		// System.out.println(hand.finger(1).isExtended());
		// System.out.println(thumb.cross(fingerDirs.get(1)));

		return "Y";
	}

	private String hTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		// System.out.println(fingerLoc.get(1).distanceTo(fingerLoc.get(2)));
		if (fingerDirs.get(2).getX() > 1.0
				&& fingerLoc.get(1).distanceTo(fingerLoc.get(2)) < 5) {
			return "H";
		} else if (fingerDirs.get(2).getX() > -0.3
				&& fingerLoc.get(1).distanceTo(fingerLoc.get(2)) < 5) {
			return "U";
		} else if (fingerDirs.get(2).getX() > -0.3
				&& fingerLoc.get(1).distanceTo(fingerLoc.get(2)) > 5) {
			if (hand.fingers().fingerType(Finger.Type.TYPE_THUMB).get(0)
					.isExtended()) {
				return "K";
			} else {
				return "V";
			}
		}
		return null;
	}

	private String wTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		if (fingerLoc.get(1).distanceTo(fingerLoc.get(2)) > 5
				&& fingerLoc.get(2).distanceTo(fingerLoc.get(3)) > 5) {
			return "W";
		}
		return null;
	}

	private String iTest(ArrayList<Vector> fingerDirs,
			ArrayList<Vector> fingerLoc) {
		Vector thumb = fingerDirs.get(0);
		// System.out.println(hand.finger(1).isExtended());
		// System.out.println(thumb.cross(fingerDirs.get(1)));

		return "I";
	}

}
