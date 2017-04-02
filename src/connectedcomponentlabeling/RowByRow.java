/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import LibraryLB.Log;
import connectedcomponentlabeling.ConnectedComponentLabeling.MiniComponent;
import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import connectedcomponentlabeling.ConnectedComponentLabeling.iTableFunction;
import static connectedcomponentlabeling.ConnectedComponentLabeling.printLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.tableFunction;
import connectedcomponentlabeling.OptimizedAPI.MiniShared;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Lemmin
 */
public class RowByRow {
    
    public static HashSet<LabelRelation> relations = new HashSet<>();
    public static HashMap<String,String> lookUp = new HashMap<>();
    public static ArrayList<HashSet<HashMap>> ok = new ArrayList<>();
    public static class LabelRelation{
        public String l1,l2,dedicatedLabel;
        public LabelRelation(String label1,String label2){
            l1 = label1;
            l2 = label2;
        }
        public String toString(){
            return l1+" <-> "+l2 + " = "+dedicatedLabel;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LabelRelation other = (LabelRelation) obj;
            return other.hashCode() == this.hashCode();
        }
        @Override
        public int hashCode(){
            return this.l1.hashCode() + this.l2.hashCode();
        }
        
        public boolean isPartOf(String label){
            return l1.equals(label)|| l2.equals(label);
        }
    }
    public static void parseRow(MiniShared shared, final int rowIndex){
        MiniComponent current = shared.get(rowIndex, 0);
        MiniComponent next = current;
        MiniComponent top;
        String label = getUnusedLabel();
        if(rowIndex>0){
            for(int i=1; i<shared.length(); i++){
                next = shared.get(rowIndex, i);
                top = shared.get(rowIndex-1,i-1);
                current.label = label;
                              
                if(current.id == top.id){//collision
                    relations.add(new LabelRelation(current.label,top.label));
                }
                if(current.id != next.id){
                    label = getUnusedLabel();
                }  
                current = next;
            } 
        }else{
            for(int i=1; i<shared.length(); i++){
                next = shared.get(rowIndex, i);
                
                current.label = label;
                if(current.id != next.id){
                    label = getUnusedLabel();
                }
                current = next;
            }
            
        }
        next.label = label;
        
    }
            
    public static HashSet<LabelRelation> minimizeRelations(HashSet<LabelRelation> relation){
        ArrayDeque<LabelRelation> list = new ArrayDeque<>();
        list.addAll(relation);
        HashSet<LabelRelation> newSet = new HashSet<>();
        int lab = 1;
        while(!list.isEmpty()){
            LabelRelation first = list.pollFirst();
            newSet.addAll(recursiveRelation(first,list,lab+""));
            lab++;
        }
        return newSet;
        
    }
    
    public static ArrayList<LabelRelation> recursiveRelation(LabelRelation rel, ArrayDeque<LabelRelation> list,String dedicatedLabel){
        ArrayList<LabelRelation> connected = new ArrayList<>();
        connected.add(rel);
        rel.dedicatedLabel = dedicatedLabel;
        ArrayDeque<LabelRelation> copy = new ArrayDeque();
        copy.addAll(list);
        ArrayDeque<LabelRelation> related = new ArrayDeque<>();
        for(LabelRelation next:copy){
            if(next.isPartOf(rel.l1) || next.isPartOf(rel.l2)){
                related.add(next);
                list.remove(next);
            }
        }
        for(LabelRelation next:related){
            connected.addAll(recursiveRelation(next,list,dedicatedLabel));
        }
        return connected;
    }
    public static void strat(MiniShared shared){
        for(int i=0; i<shared.width(); i++){
            parseRow(shared,i);
        }
        iTableFunction relabel = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                MiniComponent comp = (MiniComponent) array[x][y];
                String lab = comp.label;
                if(lookUp.containsKey(lab)){
                    comp.label = lookUp.get(lab);
                }
                
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
            }
        };
        
//        
//        Log.print();
//        for(LabelRelation rel:minimized){
//            Log.print(rel);
//        }
//        tableFunction(shared.comp,printLabel);
        long time = System.currentTimeMillis();
        HashSet<LabelRelation> minimized = minimizeRelations(relations);
        Log.print(System.currentTimeMillis() - time + " minimizeTime");
        relations = minimized;
        for(LabelRelation rel:relations){
            lookUp.put(rel.l1, rel.dedicatedLabel);
            lookUp.put(rel.l2, rel.dedicatedLabel);
        }
        
        tableFunction(shared.comp,relabel);
        
        
    
    };
}
