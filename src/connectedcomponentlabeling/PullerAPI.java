/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.join;
import static connectedcomponentlabeling.ConnectedComponentLabeling.start;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author Lemmin
 */
public class PullerAPI {
        public static boolean print = false;
        public static class Store{
        public static ArrayList<Store> connected = new ArrayList<>();
        public HashSet<ConnectedComponentLabeling.Component> stored;
        public HashSet<ConnectedComponentLabeling.Pos> compareTo;
        public HashSet<ConnectedComponentLabeling.Pos> currentBottom;
        public boolean firstPush;
        public Store(){
            firstPush = true;
            stored = new HashSet<>();
            compareTo = new HashSet<>();
            currentBottom = new HashSet<>();
        }
    }
    
    public static class HorizontalPuller extends ConnectedComponentLabeling.Worker{
        public int index;
        public HorizontalPuller top;
        public volatile CountDownLatch latch;
        public ArrayList<ConnectedComponentLabeling.Pos> iterated = new ArrayList<>();
        public ArrayList<Collection<ConnectedComponentLabeling.Component>> pulled = new ArrayList<>();
        public HorizontalPuller(ConnectedComponentLabeling.Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
            latch = new CountDownLatch(1);
            
        }
        
        private void iterateUntillWall(){
            ArrayList<ConnectedComponentLabeling.Component> set = new ArrayList<>();
            
            ConnectedComponentLabeling.Component current = get(constant,index);
            iterated.add(current.location);
            set.add(current);
            while(current.right!=null){
                current = get(current.right);
                set.add(current);
                index++;
                iterated.add(current.location);
            }
            pulled.add(set);
            
        }
        
        private ArrayList<ConnectedComponentLabeling.Pos> getTopPos(Collection<ConnectedComponentLabeling.Component> list){
            ArrayList<ConnectedComponentLabeling.Pos> set = new ArrayList<>();
            for(ConnectedComponentLabeling.Component c:list){
                ConnectedComponentLabeling.Component co = get(c.location.y-1,c.location.x);
                if(co != null && co.down!= null){
                    set.add(co.location);
                }
            }
            return set;
        }
        private void createNewStore(Collection<ConnectedComponentLabeling.Component> list){
            Store store = new Store();
            for(ConnectedComponentLabeling.Component c:list){
                store.compareTo.add(c.location);
                store.currentBottom.add(c.location);
                
            }
            store.stored.addAll(list);
            Store.connected.add(store);
            if(print)
            System.out.println("Store created! "+Store.connected.size() + " "+store.stored);
        }
        public <T> boolean hasSameElement(Collection<T> col1, Collection<T> col2){
            for(T el:col1){
                if(col2.contains(el)){
                    return true;
                }
            }
            return false;
        }
        @Override
        public Object call() throws Exception {
            while(index<this.length){
                iterateUntillWall();
                index++;
                if(get(constant,index) == null){
                    break;
                }
            }
            
            System.out.println(constant+" : "+this.pulled);
            
            if(top==null){// I AM THE FIRST ONE
                System.out.println(constant+" : START First");
                for(Collection<ConnectedComponentLabeling.Component> map:pulled){
                    createNewStore(map);
                }
                
            }else{
                top.latch.await();
                 // wait to finish
                System.out.println(constant+" : START");
                for(Store s:Store.connected){
                    s.currentBottom.clear();
                    s.firstPush = true;
                }
                for(Collection<ConnectedComponentLabeling.Component> map:pulled){
                    boolean needNewStore = true;
                    boolean storeCondition = true;
                    boolean quickEnd = false;
                    int storeIndex = 0;
                    while(!quickEnd && (storeIndex<Store.connected.size())){
                        Store store = Store.connected.get(storeIndex);
                        storeIndex++;
                        ArrayList<ConnectedComponentLabeling.Pos> topPos = getTopPos(map);
                        ArrayList<ConnectedComponentLabeling.Pos> currentPosistions = new ArrayList<>(map.size());
                            for(ConnectedComponentLabeling.Component c:map){
                                currentPosistions.add(c.location);
                            }
                        if(topPos.isEmpty()){
//                            System.out.println("Empty "+map);
                            needNewStore = true;
                            quickEnd = true;
                        }else
                        if(!topPos.isEmpty() && hasSameElement(topPos,store.compareTo)){//has atleast 1 same element
                            
                            needNewStore = false;
//                            System.out.println("MERGE :"+currentPosistions +" |"+store.compareTo);
                            if(store.firstPush){
                                
                                store.compareTo.addAll(currentPosistions);
                                store.currentBottom.addAll(currentPosistions);
                                store.firstPush = false;
                            }else{
                                store.compareTo.addAll(currentPosistions);
                                store.currentBottom.addAll(currentPosistions);
                            }
                            store.stored.addAll(map);
                            
                        }else{
//                            System.out.println("NSE :"+currentPosistions +" |"+store.compareTo);
                        }
                    }
                    if(needNewStore){
                        createNewStore(map);
                    }
                    
                }
                
                for(Store s:Store.connected){
                    s.compareTo.clear();
                    s.compareTo.addAll(s.currentBottom);
                    s.currentBottom.clear();
                    s.firstPush = true;
                }
                
                // Try to merge store
                ArrayList<Store> newStores = new ArrayList<>();
                for(int i = Store.connected.size()-1; i >= 0; i--){
                    Store s1 = Store.connected.get(i);
                    
                    
                    for(int j = i-1; j>=0; j--){
                        
                        Store s2 = Store.connected.get(j);
                        if(hasSameElement(s1.stored, s2.stored)){//contains 1 same elemnt, needs merging
                            if(print){
                                System.out.println("Store "+i +" "+j+" merge!");
                                System.out.println(s1.stored);
                                System.out.println(s2.stored);
                            }
                            
                            
                            Store.connected.remove(j);
                            i = j;
                            s1.stored.addAll(s2.stored);
                            s1.currentBottom.addAll(s2.currentBottom);
                            s1.compareTo.addAll(s2.compareTo);
                            
                        }
                    }
                }
                
            }
            latch.countDown();
            if(print){
               System.out.println("Stores:");
                for(Store s:Store.connected){
                    System.out.println(s.stored);
                    System.out.println(s.compareTo);
                }
                System.out.println("##############################");
                System.out.println(constant+" : "+Store.connected);

                System.out.println("END:"+this.constant+" "+latch.getCount()); 
            }
            
            
//            System.out.println(latch.getCount());
            return null;
        }
        
    }
     public static void advancedStrategy(ConnectedComponentLabeling.Shared shared) throws InterruptedException{
        ArrayList<HorizontalPuller> pullers = new ArrayList<>(shared.width);
        HorizontalPuller current = new HorizontalPuller(shared.comp,0);
        pullers.add(current);
        for(int i=1 ; i<shared.width; i++){
            HorizontalPuller pull = new HorizontalPuller(shared.comp,i);
            pull.top = current;
            current = pull;
            pullers.add(pull);
        }
        start(pullers);
        join(pullers);
        for(Store store:Store.connected){
            String unusedLabel = getUnusedLabel();
            for(ConnectedComponentLabeling.Component component:store.stored){
                if(component == null){
                    System.out.println("Found null");
                }else{
                    component.label = unusedLabel;
                }
                
            }
        }
//        HorizontalPuller get = pullers.get(0);
//        System.out.println(get.iterated);
    }
}
