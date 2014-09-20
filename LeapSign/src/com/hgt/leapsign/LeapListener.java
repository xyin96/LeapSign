package com.hgt.leapsign;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.print.attribute.standard.Media;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Vector;
import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;
import com.swabunga.spell.jedit.JazzyPlugin;
import com.swabunga.spell.jedit.JazzySpellCheck;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;

public class LeapListener extends Listener implements SpellCheckListener{
	private final String API_KEY = "AIzaSyDUIHqw1okuAQ-XZCse3yQU3U23FVa90jE";
	private Hand hand;
	private String str, filter;
	private StringBuilder word;
	private int fcount = 0;
	private Clip clip;
	private SpellChecker spellCheck;
	private BasicPlayer player;

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

				SpellDictionary dictionary = null;
				try {
					dictionary = new SpellDictionaryHashMap(new File(
							"C:\\Users\\Xiaoyu\\workspace\\LeapSign\\words"));
				} catch (FileNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if (dictionary != null) {
					if (spellCheck == null) {
						spellCheck = new SpellChecker(dictionary);
						spellCheck.addSpellCheckListener(this);
					}
					spellCheck.checkSpelling(new StringWordTokenizer(filter));

					System.out.println("filter: " + filter);

					URL url = null;
					try {
						url = new URL(
								"http://translate.google.com/translate_tts?ie=UTF-8&tl=en&q="
										+ URLEncoder.encode(filter));
						URLConnection connection = url.openConnection();
						connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
						InputStream in = new BufferedInputStream(
								connection.getInputStream());
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int n = 0;
						while (-1 != (n = in.read(buf))) {
							out.write(buf, 0, n);
						}
						out.close();
						in.close();
						byte[] response = out.toByteArray();

						FileOutputStream fos;
						fos = new FileOutputStream(
								"C:\\Users\\Xiaoyu\\workspace\\LeapSign\\tts.mp3");
						fos.write(response);
						fos.close();
						
						String pathToMp3 = "C:\\Users\\Xiaoyu\\workspace\\LeapSign\\tts.mp3";
						try {
							if(player == null){
								player = new BasicPlayer();
							}
						    player.open(new URL("file:///" + pathToMp3));
						    player.play();
						} catch (BasicPlayerException | MalformedURLException e) {
						    e.printStackTrace();
						}
						
						

					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

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
		// System.out.println(fVel + "");
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

	@Override
	public void spellingError(SpellCheckEvent event) {
		List suggestions = event.getSuggestions();
		if (suggestions.size() > 0) {
			System.out.println("MISSPELT WORD: " + event.getInvalidWord());
			for (Iterator suggestedWord = suggestions.iterator(); suggestedWord
					.hasNext();) {
				System.out.println("\tSuggested Word: " + suggestedWord.next());
			}
		} else {
			System.out.println("MISSPELT WORD: " + event.getInvalidWord());
			System.out.println("\tNo suggestions");
		}

	}

}
