/**
 * @author <a href="mailto:gery.casiez@lifl.fr">Gery Casiez</a>
 */

import java.awt.Point;
import java.util.Vector;
import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import java.util.List;
import java.util.Iterator;


public class TemplateManager
{
	private Vector<Template> theTemplates;
	private org.jdom.Document document;
	private Element racine;
	private String fileName;
	
	TemplateManager(String filename) {
		theTemplates = new Vector<Template>();
		this.fileName = filename;
		loadFile(filename);
	}
	
	void loadFile(String filename) {
		SAXBuilder sxb = new SAXBuilder();
		
		// Open xml file
	    try {
	    	document = sxb.build(new File(filename));
	    }
	    catch(Exception e){
	    	System.out.println("Problem loading file");
	    	System.exit(-1);
	    }
	    racine = document.getRootElement();
	    
	    List listTemplates = racine.getChildren("template");

	    Iterator i = listTemplates.iterator();
	    // Go through each template
	    while(i.hasNext())
	    {
	    	// Get template name
	    	Element courant = (Element)i.next();
	    	String name = courant.getAttributeValue("name").toString();

	    	// Get all points
	    	Vector<PointData> pts = new Vector<PointData>();
	    	Iterator c = courant.getChildren().iterator();
	    	while (c.hasNext()) {
	    	   Element point = (Element)c.next();
	    	   pts.add(new PointData(new Point(Integer.parseInt(point.getAttributeValue("x").toString()),
	    			   Integer.parseInt(point.getAttributeValue("y").toString())),
	    			   Long.parseLong(point.getAttributeValue("ts").toString())));
	       }
	    	theTemplates.add(new Template(name, pts));
	    }		
	}
	
	Vector<Template> getTemplates() {
		return theTemplates;
	}
}