package prismsoul.genealogy.gedgraph;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

import org.gedcom4j.model.Family;
import org.gedcom4j.model.FamilyChild;
import org.gedcom4j.model.Gedcom;
import org.gedcom4j.model.Individual;
import org.gedcom4j.model.IndividualAttribute;
import org.gedcom4j.model.IndividualAttributeType;
import org.gedcom4j.model.IndividualEvent;
import org.gedcom4j.model.IndividualEventType;
import org.gedcom4j.model.PersonalName;
import org.gedcom4j.parser.GedcomParser;
import org.gedcom4j.parser.GedcomParserException;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.Viewer;


public class GraphRepresent extends JFrame
{
	public static enum Relationship {FATHER, MOTHER, SPOUSE};

	/**
	 * @param args
	 * @throws GedcomParserException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, GedcomParserException
	{
		if (args.length != 3)
		{
			System.out.println("Usage: <filename> <surname> <givenname>");
			System.exit(0);
		}
		// TODO Auto-generated method stub
		Graph graph = new SingleGraph("Test");

		GedcomParser gp = new GedcomParser();
	    gp.load(args[0]);
	    boolean found = false;
	    for (Individual individual : gp.gedcom.individuals.values())
		{
			if ((!individual.names.isEmpty()))
			{
				PersonalName name = individual.names.get(0);
//				System.out.println(name.basic + " surname " + name.surname + " given " + name.givenName);
				if ((name.surname != null) && (name.givenName != null) && args[1].equalsIgnoreCase(name.surname.value) && args[2].equalsIgnoreCase(name.givenName.value))
				{
					explore(graph, null, null, gp.gedcom, individual, new HashSet<String>(), 0);
					found = true;
					break;
				}
			}
		}

	    if (found)
	    {
	    	String styleSheet = getResourceToString("/prismsoul/genealogy/gedgraph/ui.stylesheet");
	    	graph.addAttribute("ui.stylesheet", styleSheet);
	    	Viewer viewer = graph.display();
	    	//viewer.enableAutoLayout(new HierarchicalLayout());
	    }
	    else
	    	System.out.println("Could not find individual " + args[1] + " " + args[2] + " in file " + args[0]);
	}

	private static String getResourceToString(String pResource)
	{
		try
		{
			StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader(new InputStreamReader(GraphRepresent.class.getResourceAsStream(pResource), "UTF-8"));
			for (int c = br.read(); c != -1; c = br.read())
				sb.append((char)c);
			return br.toString();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void explore(Graph pGraph, Node pParentNode, Relationship pRel, Gedcom pGedcom, Individual ind, Set<String> pAlreadyExplored, int pLevel)
	{
		if (pAlreadyExplored.contains(ind.recIdNumber.value))
			return;
		pAlreadyExplored.add(ind.recIdNumber.value);
		if (!ind.names.isEmpty())
		{
			for (int i = 0; i < pLevel; i++)
				System.out.print("\t");
			PersonalName name = ind.names.get(0);
			String birth = "";
			String death = "";
			String bplace = "";
			String dplace = "";
			for (IndividualEvent event : ind.events)
			{
				if (IndividualEventType.BIRTH.equals(event.type))
				{
					if (event.date != null) birth = event.date.value;
					if (event.place != null) bplace = event.place.placeName;
				}
				if (IndividualEventType.BAPTISM.equals(event.type))
				{
					if ("".equals(birth))
						if (event.date != null) birth = event.date.value;
					if ("".equals(bplace))
						if (event.place != null) bplace = event.place.placeName;
				}
				if (IndividualEventType.DEATH.equals(event.type))
				{
					if (event.date != null) death = event.date.value;
					if (event.place != null) dplace = event.place.placeName;
				}
				if (IndividualEventType.BURIAL.equals(event.type))
				{
					if ("".equals(death))
						if (event.date != null) death = event.date.value;
					if ("".equals(bplace))
						if (event.place != null) dplace = event.place.placeName;
				}
			}
			if (!bplace.isEmpty())
				bplace = " in " + bplace.split(",")[0];
			if (!dplace.isEmpty())
				dplace = " in " + dplace.split(",")[0];
			if ((birth.length() > 0) || (bplace.length() > 0))
				birth = "\nB " + birth + bplace;
			if ((death.length() > 0) || (dplace.length() > 0))
				death = "\nD " + death + dplace;
			String occupation = "";
			for (IndividualAttribute attr : ind.attributes)
			{
				if (IndividualAttributeType.OCCUPATION.equals(attr.type))
					if (attr.description != null)
					{
						occupation = "\n" + attr.description.value;
						break;
					}
			}
			Node node = pGraph.addNode(ind.recIdNumber.value);
			node.addAttribute("ui.label", ind.formattedName() + birth + death + occupation);
			//Node node = pGraph.addNode(ind.formattedName() + " (" + birth + bplace + " - " + death + dplace + ") " + occupation);
			if (pParentNode != null)
			{
				Edge edge = pGraph.addEdge(pRel.toString() + pParentNode.getId(), pParentNode, node);
			}
			System.out.println(ind.formattedName() + " (" + birth + bplace + " - " + death + dplace + ") " + occupation);
			for (FamilyChild fc : ind.familiesWhereChild)
			{
				Family f = fc.family;
				if (f.husband != null)
				{
					explore(pGraph, node, Relationship.FATHER, pGedcom, f.husband, pAlreadyExplored, pLevel + 1);
				}
				if (f.wife != null)
				{
					explore(pGraph, node, Relationship.MOTHER, pGedcom, f.wife, pAlreadyExplored, pLevel + 1);
				}
			}
//			for (Individual spouse : ind.getSpouses())
//			{
//				explore(pGraph, node, Relationship.SPOUSE, pGedcom, spouse, pAlreadyExplored, pLevel);
//				//pGraph.
//			}
		}
	}

	private static void exploreCLI(Gedcom pGedcom, Individual ind, Set<String> pAlreadyExplored, int pLevel)
	{
		if (pAlreadyExplored.contains(ind.recIdNumber.value))
			return;
		pAlreadyExplored.add(ind.recIdNumber.value);
		if (!ind.names.isEmpty())
		{
			for (int i = 0; i < pLevel; i++)
				System.out.print("\t");
			PersonalName name = ind.names.get(0);
			String birth = "?";
			String death = "?";
			String bplace = "";
			String dplace = "";
			for (IndividualEvent event : ind.events)
			{
				if (IndividualEventType.BIRTH.equals(event.type))
				{
					if (event.date != null) birth = event.date.value;
					if (event.place != null) bplace = event.place.placeName;
				}
				if (IndividualEventType.BAPTISM.equals(event.type))
				{
					if ("?".equals(birth))
						if (event.date != null) birth = event.date.value;
					if ("".equals(bplace))
						if (event.place != null) bplace = event.place.placeName;
				}
				if (IndividualEventType.DEATH.equals(event.type))
				{
					if (event.date != null) death = event.date.value;
					if (event.place != null) dplace = event.place.placeName;
				}
				if (IndividualEventType.BURIAL.equals(event.type))
				{
					if ("?".equals(death))
						if (event.date != null) death = event.date.value;
					if ("".equals(bplace))
						if (event.place != null) dplace = event.place.placeName;
				}
			}
			if (!bplace.isEmpty())
				bplace = " in " + bplace;
			if (!dplace.isEmpty())
				dplace = " in " + dplace;
			String occupation = "";
			for (IndividualAttribute attr : ind.attributes)
			{
				if (IndividualAttributeType.OCCUPATION.equals(attr.type))
					if (attr.description != null)
						occupation = attr.description.value;
			}
			System.out.println(ind.formattedName() + " (" + birth + bplace + " - " + death + dplace + ") " + occupation);
			for (FamilyChild fc : ind.familiesWhereChild)
			{
				Family f = fc.family;
				if (f.husband != null)
					exploreCLI(pGedcom, f.husband, pAlreadyExplored, pLevel + 1);
				if (f.wife != null)
					exploreCLI(pGedcom, f.wife, pAlreadyExplored, pLevel + 1);
			}
			for (Individual spouse : ind.getSpouses())
				exploreCLI(pGedcom, spouse, pAlreadyExplored, pLevel);
		}
	}
}
