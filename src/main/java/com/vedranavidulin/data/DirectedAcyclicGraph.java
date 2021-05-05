/*
 * Copyright (c) 2021 Vedrana Vidulin
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.vedranavidulin.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Vedrana Vidulin
 */
public class DirectedAcyclicGraph {
    private final int numNodes;
    private List<Integer>[] adjacentNodes;
    private List<List<Integer>> allPaths;
    
    public DirectedAcyclicGraph(int numNodes) { 
        this.numNodes = numNodes;
        adjacentNodes = new ArrayList[numNodes]; 
        for (int i = 0; i < numNodes; i++)
            adjacentNodes[i] = new ArrayList<>();
    }
    
    public void addEdge(int startingNode, int endingNode) { 
        adjacentNodes[startingNode].add(endingNode);
    }
    
    public List<List<Integer>> findAllPaths(int fromNode, int toNode) {
        allPaths = new ArrayList<>();
        boolean[] isVisited = new boolean[numNodes]; 
        List<Integer> pathList = new ArrayList<>(); 
        pathList.add(fromNode); 
        printAllPathsUtil(fromNode, toNode, isVisited, pathList);
        
        return allPaths;
    }
    
    private void printAllPathsUtil(Integer fromNode, Integer toNode, boolean[] isVisited, List<Integer> localPathList) { 
        isVisited[fromNode] = true; 
          
        if (fromNode.equals(toNode)) {
            allPaths.add(new ArrayList<>(localPathList));
            isVisited[fromNode]= false;
            return;
        }
        
        for (Integer i : adjacentNodes[fromNode]) 
            if (!isVisited[i]) { 
                localPathList.add(i);
                printAllPathsUtil(i, toNode, isVisited, localPathList);
                localPathList.remove(i);
            }

        isVisited[fromNode] = false; 
    } 
}
