/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import connectedcomponentlabeling.ConnectedComponentLabeling.MiniComponent;
import connectedcomponentlabeling.ConnectedComponentLabeling.Pos;
import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.join;
import static connectedcomponentlabeling.ConnectedComponentLabeling.printLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.sortByComponent;
import static connectedcomponentlabeling.ConnectedComponentLabeling.sortByPos;
import static connectedcomponentlabeling.ConnectedComponentLabeling.start;
import static connectedcomponentlabeling.ConnectedComponentLabeling.tableFunction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;


/**
 *
 * @author Lemmin
 */
public class OptimizedAPI {
    
    public static MiniComponent[][] fromPixelArrayMini(Integer[][] pixels){
        int width = pixels.length;
        int length = pixels[0].length;
        MiniComponent[][] array = new MiniComponent[width][length];
        for(int i=0;i<width;i++){
            for(int j=0; j<length; j++){
                MiniComponent comp = new MiniComponent(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
   
    public static class CompSet{
        public HashSet<Pos> topPos;
        public HashSet<MiniComponent> bottom;
        public HashSet<MiniComponent> collected;
        public HashSet<Pos> recentlyAdded;
        public boolean added = false;
        public HashSet<Pos> getConnectedBottomPos(){
            HashSet<Pos> pos = new HashSet<>();
            bottom.forEach(component ->{
                if(component.down != null){
                    pos.add(component.down);
                }
            });
            return pos;
        }
        public CompSet(){
            this.topPos = new HashSet<>();
            this.bottom = new HashSet<>();
            this.collected = new HashSet<>();
            this.recentlyAdded = new HashSet<>();
        }
        @Override
        public String toString(){
            String res = "" ;
            res += "top:\n" + sortByPos(topPos)+"\n";
            res += "collected:\n"+ sortByComponent(collected)+"\n";
            res += "bottom:\n"+sortByPos(getConnectedBottomPos());
            return res;
        }
    }
    public static class MiniShared{
        public MiniComponent[][] comp;
        public final int length,width;
        public MiniShared(MiniComponent[][] array){
            this.comp = array;
            this.width = array.length;
            this.length = array[0].length;
        }
        protected MiniComponent get(int y, int x){
            if(y<this.width && x<this.length){
                return this.comp[y][x];
            }else{
                return null;
            }
        }
        protected MiniComponent get(Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    public static class UltimateWorker extends MiniShared implements Callable{
        private final int workLine;
        public ArrayDeque<HashSet<MiniComponent>> iterated;
        public ArrayList<CompSet> compSet;
        public UltimateWorker(MiniComponent[][] array, int workLine) {
            super(array);
            this.workLine = workLine;
            this.compSet = new ArrayList<>();
            this.iterated = new ArrayDeque<>();
            
        }


        @Override
        public Object call() throws Exception {
            int index = 0;
            this.iterated.add(new HashSet<>());
            MiniComponent prev;
            MiniComponent next = this.comp[workLine][index];
            MiniComponent down = this.get(workLine+1, index);
            if(down!=null && down.id == next.id){
                next.down = down.location;
            }
            index++;
            while(index<this.length){
                this.iterated.getLast().add(next);
                prev = next;
                next = this.get(workLine, index);
                if(prev.id != next.id){
                    this.iterated.add(new HashSet<>());
                }
                down = this.get(workLine+1, index);
                if(down!=null && down.id == next.id){
                    next.down = down.location;
                }
                index++;
            }
            for(HashSet<MiniComponent> set:iterated){
                CompSet cset = new CompSet();
                cset.bottom.addAll(set);
                set.forEach(component ->{
                    cset.topPos.add(component.location);
                });
                cset.collected.addAll(set);
                compSet.add(cset);
            }
            return null;
        }
        
    }
    
    
    public static class WorkerMerger implements Callable{
        public CountDownLatch latch;
        public WorkerMerger depTop,depBot;
        public UltimateWorker top,bot;
        public WorkerMerger(UltimateWorker top, UltimateWorker bottom) {
            this.top = top;
            this.bot = bottom;
        }
        
        public <T> boolean hasSameElement(Collection<T> col1, Collection<T> col2){
            if(col1.isEmpty()||col2.isEmpty()){
                return false;
            }
            if(col1.size()<col2.size()){
                if (col1.stream().anyMatch((el) -> (col2.contains(el)))) {
                    return true;
                }
            }else{
                if (col2.stream().anyMatch((el) -> (col1.contains(el)))) {
                    return true;
                } 
            }
            
            return false;
        }
        
        public void waitForDependenciess() throws InterruptedException{
            if(depTop!=null){
                depTop.latch.await();
            }
            if(depBot!=null){
                depBot.latch.await();
            }
        }
        
        private void logic(){
            ArrayList<CompSet> topSet = top.compSet;  
            ArrayList<CompSet> botSet = bot.compSet;
            int i =-1;
            int topLimit = topSet.size()-1;
            int botLimit = botSet.size()-1;
            boolean endOuter = false;
            boolean endInner = false;
            while(i<topLimit && !endOuter){
                i++;
                CompSet set = topSet.get(i);
                HashSet<Pos> connectedBottomPos = set.getConnectedBottomPos();
                
                ArrayList<CompSet> matchedSets = new ArrayList<>();
                int j = -1;
                while(j<botLimit && !endInner){
                    j++;
                    CompSet otherSet = botSet.get(j);
                    if(hasSameElement(connectedBottomPos,otherSet.topPos)){
                        matchedSets.add(otherSet);
                        otherSet.added = true;
                    }
                }
                set.bottom.clear();
                
                for(CompSet matched:matchedSets){
                    set.bottom.addAll(matched.bottom);
                    set.collected.addAll(matched.collected);
                }
                  
            }
            //find new set
            for(CompSet bots:botSet){              
                if(!bots.added){
                    topSet.add(bots);
                }
            }
//            System.out.println("SETS!!");
//            topSet.forEach(set ->{
//                System.out.println(set+"\n");
//                
//            });
            
            
            
            

        }
        @Override
        public Object call() throws Exception {
            waitForDependenciess();
//            Thread.sleep(1000);
            logic();
            latch.countDown();
            System.out.println("Finished:"+id());
            return null;
        }
        
        public String id(){
            return top.workLine+" "+bot.workLine;
        }
        @Override
        public String toString(){
            String res = id() + " deps:\n";
            if(depTop!=null){
                res+="top:"+depTop.id();
            }
            if(depBot!=null){
                res+= " bot:"+depBot.id();
            }
            
            return res;
        }
        
    }
    
    public static void optimizedStrategy(Integer[][] pixels) throws InterruptedException{
        MiniShared shared = new MiniShared(fromPixelArrayMini(pixels));
        UltimateWorker firstWorker;
        ArrayList<UltimateWorker> workers = new ArrayList<>();
        ArrayList<WorkerMerger> mergers = new ArrayList<>();
        for(int i = 0; i<shared.width; i++){
            UltimateWorker worker = new UltimateWorker(shared.comp,i);
            workers.add(worker);
            
        }
        firstWorker = workers.get(0);
        int width = shared.width;
        int increment = 2;
        HashMap<Integer,WorkerMerger> tempMerger = new HashMap<>();
        do{
            ArrayList<WorkerMerger> mergersThisTime = new ArrayList<>();
            boolean setIsEmpty = tempMerger.isEmpty();
            int offset = increment/2;
            for(int i=0; i+offset<width; i+= increment){
                int top = i;
                int bot = i + offset;
                System.out.println(top+" "+(bot));
                WorkerMerger merger = new WorkerMerger(workers.get(top),workers.get(bot));
                mergersThisTime.add(merger);
                merger.latch = new CountDownLatch(1);
                if(setIsEmpty){
                    tempMerger.put(top, merger);
                }else{
                    merger.depBot = tempMerger.remove(bot);
                    merger.depTop = tempMerger.remove(top);
                    tempMerger.put(top, merger);
                }
                
            }
            increment*=2;
            mergers.addAll(mergersThisTime);
            System.out.println("#### "+increment);
        }while(increment/2<width);
        System.out.println();
        mergers.forEach(merger ->{
            System.out.println(merger);
        });
        start(workers);
        join(workers);
        start(mergers);
        join(mergers);
        firstWorker.compSet.forEach(set ->{
            final String label = getUnusedLabel();
            set.collected.forEach( comp ->{
                comp.label = label;
            });
        });
        tableFunction(shared.comp,printLabel);
//        list.forEach(worker ->{
//            worker.iterated.forEach(set ->{
//                System.out.print(set);
//            });
//            System.out.println();
//        });
//        workers.forEach(worker ->{
//            System.out.println(worker.workLine+":");
//            worker.compSet.forEach(set ->{
//                System.out.println(set);
//            });
//            
//        });
        
    }
}
