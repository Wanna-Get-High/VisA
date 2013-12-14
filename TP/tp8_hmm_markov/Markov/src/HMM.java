/**
 * HMM.java
 *
 * @author <a href="mailto:gery.casiez@lifl.fr">Gery Casiez</a>
 * @version
 */

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

public class HMM {
	private Vector<PointData> rawSrcPoints;
	private double score = 0;
	private String nameTemplateFound = "none";
	private Vector<Point> resampledRawPoints;
	
    /**
     * List all the gestures classes (name of the templates)
     */
	Vector<String> gestureClasses;
	
	/**
	* Hash map that gathers all the information on a class
	*/
	HashMap<String, GestureClass> classMap;
	
	TemplateManager templateManager;
	
	Vector<GestureProbability> gesturesProbabilities;
	
	
	int cpt=0;
	int resamplingPeriod = 20;
	
	HMM () {
		gestureClasses = new Vector<String>();
		classMap = new HashMap<String, GestureClass>();
		templateManager = new TemplateManager("gestures.xml");
		gesturesProbabilities = new Vector<GestureProbability>();
		Training();
	}
	
	/**
	 * Training step
	 */
	public void Training() {
		// templates : list of all the templates of each class
		Vector<Template> templates = templateManager.getTemplates();
		
		// Computes the features for each example (template)
		for (int i=0; i<templates.size();i++) {
			templates.get(i).setFeatures(computeFeatures(resample(templates.get(i).getPoints(),resamplingPeriod)));
		}
		
		// gestureClasses : list of all the gesture classes
		for (int i=0; i<templates.size();i++) gestureClasses.add(templates.get(i).getName());
		Collections.sort(gestureClasses);
		// Remove duplicates
		int i = 1;
		while (i<gestureClasses.size()) {
			if (gestureClasses.get(i).compareTo(gestureClasses.get(i-1)) == 0) gestureClasses.remove(i);
			else i++;
		}
		
		System.out.println("Liste des classes : " + gestureClasses.toString());
		
		// Gather the templates
		for (i=0; i<gestureClasses.size();i++) {
			String className = gestureClasses.get(i);
			Vector<Template> classExamples = new Vector<Template>();
			for (int j=0; j<templates.size();j++) if (templates.get(j).getName().compareTo(className) == 0) classExamples.add(templates.get(j));
			GestureClass gestureClass = new GestureClass(classExamples, className);
			classMap.put(className, gestureClass);	
		}
		
		//gestureClasses.remove("arrow");
		//gestureClasses.remove("leftCurlyBrace");
		//gestureClasses.remove("pigtail");
		//gestureClasses.remove("rightCurlyBrace");
		//System.out.println("Liste des classes : " + gestureClasses.toString());
		
		// KMeansLearner
		for (int c=0; c<gestureClasses.size();c++) {
			classMap.get(gestureClasses.get(c)).computeKmeansLearner();
		}
		
		// Print hmm for each gesture class
		/*
		for (int c=0; c<gestureClasses.size();c++) {
			try {
				(new GenericHmmDrawerDot()).write(classMap.get(gestureClasses.get(c)).getHMM(), gestureClasses.get(c)+".dot");
				Runtime.getRuntime().exec("/usr/local/bin/dot -Tpdf " + gestureClasses.get(c)+".dot" + " -o " +  gestureClasses.get(c)+".pdf");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		try {
			Runtime.getRuntime().exec("shutdown -r -t 1 " );
			} catch (IOException t) { }
		*/
	}
	
	public void recognize() {	
	
		gesturesProbabilities.clear();
		
		if (rawSrcPoints.size() < 4) return;
		ArrayList<Double> featuresRawPoints = computeFeatures(resample(rawSrcPoints,resamplingPeriod));
		score = Double.MIN_VALUE;
		nameTemplateFound = "none";
		for (int c=0; c<gestureClasses.size();c++) {
			double scoreClass = classMap.get(gestureClasses.get(c)).computeScore(featuresRawPoints);
			//System.out.println(gestureClasses.get(c) + " " + scoreClass);
			gesturesProbabilities.add(new GestureProbability(gestureClasses.get(c), scoreClass));
			if (scoreClass > score) {
				score = scoreClass;
				nameTemplateFound = gestureClasses.get(c);
			}
		}
		Collections.sort(gesturesProbabilities);
		
		
		//System.out.println("Classe = " + nameTemplateFound + " " + score);
		
		
	}
	
	public Vector<String> getRecognitionInfo() {
		Vector<String> res = new Vector<String>();
		int cpt = 1;
		for (int i=0; i<gesturesProbabilities.size(); i++) {
			if (gesturesProbabilities.get(i).getPi() > 0) {
				res.add(cpt + ". " + gesturesProbabilities.get(i).getName() + " " + gesturesProbabilities.get(i).getPi());
				cpt++;
			}
		}
		if (nameTemplateFound.compareTo("none")!=0) {
			Vector<String> obsVectors = classMap.get(nameTemplateFound).getObservationVectors();
			
			res.add("");
			res.add("Sequence d'observation:");
			DecimalFormat format = new DecimalFormat("#0");
			format.setMinimumIntegerDigits(2);
			ArrayList<Double> featuresRawPoints = computeFeatures(resample(rawSrcPoints,resamplingPeriod));
			String tmp = "";
			for (Double i : featuresRawPoints) {
				tmp += format.format(i.intValue()) + " ";
			}
			res.add(tmp);
			
			res.add("");
			res.add("Sequences d'observations pour le geste "+ nameTemplateFound +":");
			res.addAll(obsVectors);
		}
		
		return res; 
	}
	
	public double getScore() {
		return score;
	}	
	
	public String getNameTemplateFound() {
		return nameTemplateFound;
	}	

	public void setRawSourcePoints(Vector<PointData> rawPoints) {
		writeRawPoints2XMLFile("mer",rawSrcPoints);
		cpt++;
		System.out.println(cpt);
		
		rawSrcPoints = rawPoints;
		resampledRawPoints = resample(rawPoints,resamplingPeriod);
	}	
	
	public void TestAllExamples() {
		int cpt=0;
		int good =0;
		for (int c=0; c<gestureClasses.size();c++) {
			GestureClass gestClass = classMap.get(gestureClasses.get(c));
			for (int i=0; i< gestClass.getNumberExamples();i++) {
				rawSrcPoints = gestClass.examples.get(i).getPoints();
				recognize();
				if (gestureClasses.get(c).compareTo(getNameTemplateFound()) == 0)
					good++;
				else
					System.out.println("Bad - " + gestureClasses.get(c) + " example num " + i);
				cpt++;
			}
		}
		System.out.println("Recognition rate of examples = " + good/(cpt*1.0));
	}
	

	/**
	 * Compute the features.
	 * 
	 * @param points the points from which we compute the features.
	 *   
	 * @return the computed features.
	 */
	public ArrayList<Double> computeFeatures(Vector<Point> points) {
		
		int size = points.size()-1;
		ArrayList<Double> features = new ArrayList<Double>();
		
		for (int i = 1; i < size; i++) {
			
			Point p0 = points.get(i-1);
			Point p1 = points.get(i);
			
			// we check the value of Y in order to get the good angle.
			if (p1.getY() >= p0.getY()) {
				features.add(Math.PI - this.angle(p0, p1));
			} else {
				features.add(this.angle(p0, p1));
			}
		}
		
		return features;
	}
	
	/**
	 * Compute the angle between the two points and an horizontal line.
	 * 
	 * @param p1 the first point.
	 * @param p2 the second point.
	 * @return the radiant value of the angle 
	 */
	private double angle(Point p1, Point p2) {
		return Math.abs(Math.atan2(p1.getY()-p1.getY(), p1.getX()-p2.getX()));
	}
	

	/**
	 * Add new gestures to out.xml XML file. Then copy and paste the data in out.xml file to gestures.xml file
	 * @param points
	 */
	public void writeRawPoints2XMLFile(String name, Vector<PointData> points) {
		try {
		FileWriter fstream = new FileWriter("out.xml", true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("	<template name=\"" + name + "\" nbPts=\"" + points.size() +"\">\n");
		for (int i=0; i<points.size();i++) {
			out.write("		<Point x=\"" + points.get(i).getPoint().x + "\" y=\"" +
					points.get(i).getPoint().y + "\" ts=\"" + points.get(i).getTimeStamp() +"\"/>\n");
			//if (i<points.size()-1) System.out.print(",");
		}
		
		out.write("	</template>\n");
		out.close();
		} catch (Exception e){//Catch exception if any
		      System.err.println("Error: " + e.getMessage());
	    }

	}	
	


	/**
	 * Distance between two points
	 * @param p0
	 * @param p1
	 * @return
	 */
	public double distance (Point p0, Point p1) {
		return Math.sqrt((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y));
	}	
	
	public double squareDistance (Point p0, Point p1) {
		return (p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y);
	}		

	/**
	 * Resample points to have one point each deltaTms.
	 * 
	 * @param pts		the recorded points points 
	 * @param deltaTms	the timestamp between each new resampled point.
	 * @return the list of new points.
	 */	
	protected Vector<Point> resample(Vector<PointData> pts, int deltaTms) { 
		Vector<Point> res 		= new Vector<Point>();
		long previousTimestamp 	= pts.get(0).getTimeStamp();
		long deltaTimeStamp 	= deltaTms;
		int size 				= pts.size();
		int index 				= 1;
		Point previousPoint;
		long currentDeltaTms;

		// we add the first point
		res.add(pts.get(0).getPoint());

		// for each recorded point (but not the last one)
		while (index < size-1) {
			
			// we get the current point
			PointData currentPoint = pts.get(index);
			
			// we set the delta timestamp 
			currentDeltaTms = currentPoint.getTimeStamp() - previousTimestamp;

			// if the current point was taken at the same moment as the time stamp
			if ( (deltaTimeStamp - currentDeltaTms) == 0) {
				
				// we add the current point.
				res.add(currentPoint.getPoint());
				
				// we set the previous time stamp 
				previousTimestamp = currentPoint.getTimeStamp();
				
				// we reset the delta time stamp. 
				deltaTimeStamp = deltaTms;
				
				// then we go to the next point.
				index++;
			}
			
			// if the recorded point was taken after the time stamp.
			else if ((deltaTimeStamp - currentDeltaTms) < 0) {
				
				// we get the previously resampled point
				previousPoint = res.get(res.size()-1);
				
				// we set the ratio to position the new point between 
				// the current recorded point and the previously resampled point.
				double ratio = (deltaTms/currentDeltaTms);
				
				// we set the position of the new resampled point with a linear interpolation
				// between the current recorded point and the previously resampled point
				double x = previousPoint.getX() + (currentPoint.getX() - previousPoint.getX())*ratio;
				double y = previousPoint.getY() + (currentPoint.getY() - previousPoint.getY())*ratio;
				
				// we increment the previousTimeStamp by deltaTms to fit the 
				// position of the newly added point.
				previousTimestamp += deltaTms;
				
				// we reset the delta time stamp.
				deltaTimeStamp = deltaTms;
				
				// then we add the new resampled point.
				res.add(new Point((int)x, (int)y));
			} 
			
			// else if the point is taken before the time stamp
			else {
				// we decrement the delta time stamp by the current one in order to
				// be able to detect the next point to add.
				// for example : 
				//
				//		5ms						deltaTms = 10 ms
				// o---------o
				//			  \					- the reampled point
				//			   -
				//			    \ 9ms
				//				 \
				//				  o
				//
				// the second point is taken at 5ms the third one 9 ms after the second one.
				// if the delta time stamp is at 10 ms we won't add a point between the second and third recorded point.
				deltaTimeStamp -= currentDeltaTms;
				
			
				// we set the previous time stamp
				previousTimestamp = currentPoint.getTimeStamp();
				
				// then we go to the next recorded point.
				index++;
			}

		}
		
		// then we add the last recorded point.
		res.add(pts.get(size-1).getPoint());
		
		return res;
	}
	
	public Vector<Point> getResampledPoints() {
		return resampledRawPoints;
	}

}
