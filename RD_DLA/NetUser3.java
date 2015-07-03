/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RD_DLA;


/*
 * Author: Anthony Sulistio  
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim
 *              network extension package.
 *              This example shows how to create user and resource
 *              entities connected via a network topology, using link
 *              and router.
 *
 */
import java.io.IOException;
import java.util.*;
import java.util.Scanner;
import java.text.Format;
import gridsim.*;
import gridsim.net.*;
import gridsim.GridSim;
import gridsim.datagrid.DataGridUser;
import gridsim.net.SimpleLink;
import eduni.simjava.*;
import gridsim.index.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class basically creates Gridlets and submits them to a
 * particular GridResources in a network topology.
 */
class NetUser3 extends GridUser {

    private int myId_;      // my entity ID
    private String name_;   // my entity name
    private GridletList list_;          // list of submitted Gridlets
    private GridletList receiveList_;   // list of received Gridlets
    private int[] resourceInfoID;
    private ArrayList GISInfoList;
    private int myGISIndex_;
    private int[][] GISMap;
    
    public int successReq;
    public int[] gridletStatus;
    public int[] hopReq;
    public int[] resourceUtil;
    public int [] redundancyCounter;
    public double [][] entropy;

    /**
     * Creates a new NetUser object
     * @param name  this entity name
     * @param totalGridlet  total number of Gridlets to be created
     * @param baud_rate     bandwidth of this entity
     * @param delay         propagation delay
     * @param MTU           Maximum Transmission Unit
     * @throws Exception    This happens when name is null or haven't
     *                      initialized GridSim.
     */
    NetUser3(String name, int totalGridlet, double baud_rate, double delay,
            int MTU, int totalResource, ArrayList GISList, double[] gridletLength, int[] gridletVector) throws Exception {
        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));

        this.name_ = name;
        this.receiveList_ = new GridletList();
        this.list_ = new GridletList();


        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);
        System.out.println("Creating a grid user entity with name = " + name + ", and id = " + this.myId_);

        // Creates a list of Gridlets or Tasks for this grid user
        System.out.println(name + ":Creating " + totalGridlet + " Gridlets");
        this.createGridlet(myId_, totalGridlet, gridletLength, gridletVector);
        this.resourceInfoID = new int[totalResource];
        this.GISInfoList = GISList;

        this.resourceUtil = new int[totalResource];
        int i;
        for (i = 0; i < totalResource; i++)
            this.resourceUtil[i] = 0;
        
        this.hopReq = new int[totalGridlet];
        for (i = 0; i < totalGridlet; i++)
            this.hopReq[i] = 0;
        
        this.gridletStatus = new int[totalGridlet];
        for (i = 0; i < totalGridlet; i++)
            this.gridletStatus[i] = 0;
        
        this.redundancyCounter = new int [totalGridlet];
        for (i = 0; i < totalGridlet; i++)
            this.redundancyCounter[i] = 0;
        
        this.successReq = 0;




    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {

        // wait for a little while for about 3 seconds.
        // This to give a time for GridResource entities to register their
        // services to GIS (GridInformationService) entity.

        super.gridSimHold(3000.0 * 5);    // wait for time in seconds
        LinkedList resList = super.getGridResourceList();
        System.out.println(resList.size());
        System.out.println();
        int i, j, k, l = 0;

        // initialises all the containers
        int totalResource = resList.size();
        int resourceID[] = new int[totalResource];
        String resourceName[] = new String[totalResource];
        // a loop to get all the resources available

        for (i = 0; i < totalResource; i++) {
            resourceID[i] = this.resourceInfoID[i];
            resourceName[i] = GridSim.getEntityName(resourceID[i]);
        }

        ResourceCharacteristics resChar;
        ArrayList resCharList = new ArrayList(totalResource);
        for (i = 0; i < totalResource; i++) {
            // Requests to resource entity to send its characteristics
            super.send(resourceID[i], GridSimTags.SCHEDULE_NOW, GridSimTags.RESOURCE_CHARACTERISTICS, this.myId_);
            // waiting to get a resource characteristics
            resChar = (ResourceCharacteristics) super.receiveEventObject();

            resourceName[i] = resChar.getResourceName();

            //System.out.println( " " +  " " + resChar.getMachineList().getMachine(0).getNumFreePE());
            resCharList.add(resChar);
            /*            
            System.out.println("Receiving ResourceCharacteristics from " + resourceName[i] +
            ", with id = " + resourceID[i] + ", and PE = "
            + resChar.getMachineList().getMachine(0).getNumPE());
             */
        }

        System.out.println();

        // get GIS Information form GISInformation
        int num_GIS = this.GISInfoList.size();
        ArrayList gisList = new ArrayList(this.GISInfoList.size());
        gisList = this.GISInfoList;
        int localGISIndex = this.myGISIndex_;
        GISInformation GIS_obj;
        GIS_obj = (GISInformation) gisList.get(localGISIndex);

        // Sync Network Topology
        int[][] adjacencyMatrix = this.GISMap;
        int[] GISAdjacency;

        // caclulate number of LA on each Node based on resource Type
        // [CPU] [RAM] [HDD] [OS]
        // [1 4] [5 10] [11 14] [15 16]
        // divide each range to subranges with two elements
        // {1,2},{3,4} {5,6}{7,8}{9,10} {11,12}{13,14} {15,16}

        int distinctType = 5;
        int maxResource = 100;
        int distinctLA = distinctType;
        
        int maxSplit = 2;
        int[] minFrequency = {0, 20, 40, 60, 80};
        int[] maxFrequency = {19, 39, 59, 79, 99};
        int[] upperbound = {9, 19, 29, 39, 49, 59, 69, 79, 89, 99};
        this.initilizeEntropy(distinctType, maxSplit);

/*        
        int maxSplit = 4;
        int[] minFrequency = {0, 20, 40, 60, 80};
        int[] maxFrequency = {19, 39, 59, 79, 99};
        int[] upperbound = {4,9,14,19, 24,29,34,39, 44,49,54,59, 64,69,74,79, 84,89,94,99};

*/         
/*        
        int maxSplit = 10;
        int[] minFrequency = {0, 20, 40, 60, 80};
        int[] maxFrequency = {19, 39, 59, 79, 99};
        int[] upperbound = {1 ,3 , 5, 7, 9,11,13,15,17,19,
                            21,23,25,27,29,31,33,35,37,39,
                            41,43,45,47,49,51,53,55,57,59,
                            61,63,65,67,69,71,73,75,77,79,
                            81,83,85,87,89,91,93,95,97,99};
*/             




        /*        
        for (i = 0; i < adjacencyMatrix.length; i++)
        {
        for (j = 0; j < adjacencyMatrix.length; j++)
        System.out.print(adjacencyMatrix[i][j]);
        System.out.println();
        }
         */

        // Initialize Learning Automata
        // construct 2D Automata Set: [resourceType] [resourceRange]
        automata automaton;
        ArrayList[][] automataList = new ArrayList[distinctLA][maxSplit];  // array
        int adjacenceGISNumel;
        int distinctRangeLength = maxSplit;
        for (i = 0; i < distinctLA; i++) {
            for (j = 0; j < distinctRangeLength; j++) {
                automataList[i][j] = new ArrayList();
                for (k = 0; k < num_GIS; k++) {
                    adjacenceGISNumel = 0;
                    for (l = 0; l < num_GIS; l++) {
                        if (adjacencyMatrix[k][l] == 1) {
                            adjacenceGISNumel++;
                        }
                    }
                    // maxSplit as an automaton index in LA set of each Node
                    automaton = new automata(i + "_" + j + "_" + k, adjacenceGISNumel);
                    automataList[i][j].add(automaton);
                }
            }
        }

        // Set Automaton ActionSet

        for (i = 0; i < distinctLA; i++) {
            for (j = 0; j < distinctRangeLength; j++) {
                for (k = 0; k < num_GIS; k++) {
                    automaton = (automata) automataList[i][j].get(k);
                    for (l = 0; l < num_GIS; l++) {
                        if (adjacencyMatrix[k][l] == 1) {
                            automaton.setAutomatonActionSet(l);
                        }
                    }
                    automataList[i][j].set(k, automaton);
                }
            }
        }

        ////////////////////////////////////////////////
        // SUBMIT Gridlets
        // determines which GridResource to send to
        // sends all the Gridlets


        System.out.println();
        Gridlet gl = null;
        boolean success;
        int successCount = 0;
        int totalGridlet = list_.size();
        // int gridletStatus[] = new int[totalGridlet];
        // for (i = 0; i < totalGridlet; i++)
        //    gridletStatus[i] = 0; // set Gridlet ststus as FAIL
        int[] memberRsource;
        Random random = new Random();   // a random generator
        int nextGIS;
        int action = -1;
        int previousGIS;
        int isVisit[] = new int[num_GIS];
        for (i = 0; i < num_GIS; i++) {
            isVisit[i] = 0;
        }
        int count = 0;
        // int [] hop = new int [totalGridlet];
        // for (i = 0; i < totalGridlet; i++)
        //    hop[i] = 0; // set hop value to zero
        int maxHop = 100; 
        int sentinel = 0;
        adjacenceGISNumel = 0;
        ArrayList gisTrace = new ArrayList();  // array
        ArrayList actionTrace = new ArrayList();  // array

        int pathLength;
        int currentAtomaton;
        int currentAction;
        int rienforcementSignal;
        int currentAutomataType = -1;
        int currentAutomataRange = -1;
        int currentRangeBound = 0;
        int rangeBreak = 0;
        int r = 0;
        int rc = 0; // redundant Counter

        // count resource utilization
        // int [] resourceUtil = new int [totalResource];
        // for (i = 0; i < totalResource; i ++)
        //    resourceUtil [i] = 0;

        //Calendar calendar = Calendar.getInstance();
        //System.out.println("Initialize simulation time = " + calendar.getTimeInMillis() + " " + GridSim.clock());
        for (i = 0; i < totalGridlet; i++) // Process Gridlets
        {
            gl = (Gridlet) this.list_.get(i);
            //System.out.println("Gridlet #" + i + ": Resource Request with " + gl.getNumPE() + " Processing Elements");
            // calculate corresponding automata set
            r = 0;
            for (j = 0; j < distinctType; j++) {
                for (k = 0; k < distinctRangeLength; k++) {
                    currentRangeBound = upperbound[r];
                    if (gl.getNumPE() <= currentRangeBound) {
                        currentAutomataType = j;
                        currentAutomataRange = k;
                        rangeBreak = 1;
                        break;
                    }
                    r++;
                }
                if (rangeBreak == 1) {
                    break;
                }
            }
            rangeBreak = 0;
            // do the search
            previousGIS = localGISIndex;
            while (this.gridletStatus[i] != 1 && sentinel != 1 && this.hopReq[i] <= maxHop) {

                if (isVisit[previousGIS] != 1) {

                    GIS_obj = (GISInformation) gisList.get(previousGIS);
                    memberRsource = convertInt(GIS_obj.myResourcelist_);

                    // Process request in local resources
                    for (j = 0; j < memberRsource.length; j++) {

                        resChar = (ResourceCharacteristics) resCharList.get(memberRsource[j]);
                        if (resChar.getNumPE() == gl.getNumPE()) {
                            success = super.gridletSubmit(gl, resChar.getResourceID(), 0.0, true);
                            // calculate resource ID
                            String str = resChar.getResourceName();
                            int indStr = str.indexOf('_') + 1;
                            String numStr = str.substring(indStr);
                            int intStr = Integer.parseInt(numStr);
                            this.resourceUtil[intStr] = 1;
                            this.gridletStatus[i] = 1; // set Gridlet ststus as SUCCESS
                            this.successReq++;
                            break;
                        }
                    }
                    this.hopReq[i]++;
                    if (this.gridletStatus[i] == 1) {
                        break;
                    }
                }
                else if (isVisit[previousGIS] == 1) {
                    this.redundancyCounter[i]++;
                }
                // check constraints
                count = 0;
                for (k = 0; k < isVisit.length; k++) {
                    if (isVisit[k] == 1) {
                        count++;
                    }
                }
                if (count == num_GIS) {
                    sentinel = 1;
                    //System.out.println("Resource Request is not found");
                }
                if (sentinel != 1) {
                    automaton = (automata) automataList[currentAutomataType][currentAutomataRange].get(previousGIS);
                    action = automaton.actionSelection();
                    nextGIS = ((Integer) automaton.actionSet_.get(action)).intValue();
                    if (isVisit[previousGIS] == 0) {
                        isVisit[previousGIS] = 1;
                        gisTrace.add(previousGIS); // keep GIS Trace
                        actionTrace.add(action);   // add action Trace
                    }
                    previousGIS = nextGIS;
                }
            }

            if (this.gridletStatus[i] == 1) {
                rienforcementSignal = 0; // reward the Request Path
            } else {
                rienforcementSignal = 1; // penalty the Request Path
            }            // Learning Automata Probability Update
            pathLength = actionTrace.size();
            for (k = 0; k < pathLength; k++) {
                currentAtomaton = ((Integer) gisTrace.get(k)).intValue();
                currentAction = ((Integer) actionTrace.get(k)).intValue();
                automaton = (automata) automataList[currentAutomataType][currentAutomataRange].get(currentAtomaton);
                GIS_obj = (GISInformation) gisList.get(currentAtomaton);
                // rienforcement signal
                automaton.probablityUpdate(currentAction, rienforcementSignal);
                automataList[currentAutomataType][currentAutomataRange].set(currentAtomaton, automaton);

            }

            sentinel = 0;
            for (k = 0; k < num_GIS; k++) {
                isVisit[k] = 0;
            }
            gisTrace.clear();
            actionTrace.clear();
        }
        /*        
        int avg = 0;
        for (i = 0; i < totalGridlet; i++)
        {
        avg += this.hopReq[i];
        //System.out.print(i + " " + gridletStatus[i] + " " + hop[i]);
        //System.out.println();
        }
        avg = avg/totalGridlet;
        System.out.println("Average Hop:" + avg);
        System.out.println("successfull Requests:" + successCount);
        
        int utilityCount = 0;
        for (i = 0; i < totalResource; i++)
        {
        if (this.resourceUtil[i] == 1)
        utilityCount++;
        }        
        System.out.println("Resource Utilization:" + utilityCount);
         */




        // System.out.println("Finalize simulation time = " + calendar.getTimeInMillis() + " " + GridSim.clock());


        ////////////////////////////////////////////////////////
        // RECEIVES Gridlets back

        // hold for few period - few seconds since the Gridlets length are
        // quite huge for a small bandwidth
        super.gridSimHold(45);

        // receives the gridlet back
        for (i = 0; i < successCount; i++)
        {
            gl = (Gridlet) super.receiveEventObject();  // gets the Gridlet
            //System.out.println("cow i am here" + gl.getGridletStatusString());
            receiveList_.add(gl);   // add into the received list
            // System.out.println(name_ + ": Receiving Gridlet #" + 
            //        gl.getGridletID() + " at time = " + GridSim.clock());
        }
        
        /// Calculate Entropy
        
        double p = 0;
        double pp = 0;
        for (i = 0; i < distinctLA; i++) {
            for (j = 0; j < distinctRangeLength; j++) {
                for (k = 0; k < num_GIS; k++)
                {
                    automaton = (automata) automataList[i][j].get(k);
                    
                    for (l = 0; l < automaton.actionNumel_; l++)
                    {
                        p = automaton.actionProbability_[l];
                        
                        pp = (double) 1/automaton.actionNumel_;
                        if (p !=0)
                            this.entropy[i][j] -= p*(Math.log(p)/Math.log(2));
                        pp -= pp*(Math.log(pp)/Math.log(2));
                        //System.out.println(pp);
                    }
                }
                this.entropy[i][j] = (this.entropy[i][j]/pp)/num_GIS;
            }
        }
/*
          for (i = 0; i < p.length; i++)
        {
            if (p[i] != 0)
                s -= p[i]*(Math.log(p[i])/Math.log(2));
        }
            System.out.println(s);
 */




        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        System.out.println(this.name_ + ": sending and receiving of Gridlets" + " complete at " + GridSim.clock());

    }

    /**
     * Gets a list of received Gridlets
     * @return a list of received/completed Gridlets
     */
    public GridletList getGridletList() {
        return receiveList_;
    }

    public void setInfo(int resInfoID[]) {
        this.resourceInfoID = resInfoID; // list of Grid Resources
    }

    public void setGISMap(int adjacencyMatrix[][]) {
        this.GISMap = adjacencyMatrix; // GIS network Topology
    }

    public void setLocalGISIndex(int index) {
        this.myGISIndex_ = index;
    }

    public int[] convertInt(ArrayList list) {
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = ((Integer) list.get(i)).intValue();
        }
        return res;
    }

    public int[] extractGISAdjacency(int[][] array, int index) {
        int adjacenceGISNumel = 0;
        int i = 0;
        for (i = 0; i < array.length; i++) {
            if (array[index][i] == 1) {
                adjacenceGISNumel++;
            }
        }
        int[] adjacenceGISIndex = new int[adjacenceGISNumel];
        int c = 0;
        for (i = 0; i < array.length; i++) {
            if (array[index][i] == 1) {
                adjacenceGISIndex[c] = i;
                // System.out.print(adjacenceGISIndex[c] + " ");
                c++;
            }
        }
        //  System.out.println();
        return adjacenceGISIndex;

    }

    public ArrayList convertArray(int[] array) {
        ArrayList list = new ArrayList(array.length);
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;

    }
    
    public void initilizeEntropy(int distinctType, int maxSplit )
    {
        int i,j;
        this.entropy = new double[distinctType][maxSplit];

        for (i = 0; i < distinctType; i++)
            for (j = 0; j < maxSplit; j++)
                this.entropy[i][j] = 0;
    }

    /**
     * This method will show you how to create Gridlets
     * @param userID        owner ID of a Gridlet
     * @param numGridlet    number of Gridlet to be created
     */
    private void createGridlet(int userID, int numGridlet, double[] gridletLength, int[] gridletPE) {
        double length;
        long file_size = 300;
        long output_size = 300;
        for (int i = 0; i < numGridlet; i++) // i as Gridlet ID
        {
            // Creates a Gridlet
            length = gridletLength[i];
            Gridlet gl = new Gridlet(i, length, file_size, output_size);
            // set gridlet component as PE in order to execute in gridsim
            gl.setNumPE(gridletPE[i]);
            gl.setUserID(userID);
            // add this gridlet into a list
            this.list_.add(gl);

        }

    }

    private void printArray(String msg, Object[] globalArray) {
        // if array is empty
        if (globalArray == null) {

            System.out.println(super.get_name() + ": number of " + msg + " = 0.");
            return;
        }

        System.out.println(super.get_name() + ": number of " + msg + " = " + globalArray.length);

        for (int i = 0; i < globalArray.length; i++) {
            Integer num = (Integer) globalArray[i];
            System.out.println(super.get_name() + ": receiving info about "
                    + msg + ", name = " + GridSim.getEntityName(num.intValue()) + " (id: " + num + ")");
        }
        System.out.println();
    }
} // end class

