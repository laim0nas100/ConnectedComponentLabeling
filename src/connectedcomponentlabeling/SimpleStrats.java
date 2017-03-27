/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import static connectedcomponentlabeling.ConnectedComponentLabeling.fromPixelArray;
import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.print;
import static connectedcomponentlabeling.ConnectedComponentLabeling.printLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.tableFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Lemmin
 */
public class SimpleStrats {
    public static Shared shared;
    public static class Shared{
        public ConnectedComponentLabeling.Component[][] comp;
        public final int length,width;
        public Shared(ConnectedComponentLabeling.Component[][] array){
            this.comp = array;
            this.width = array.length;
            this.length = array[0].length;
        }
        protected ConnectedComponentLabeling.Component get(int y, int x){
            if(y<this.width && x<this.length){
                return this.comp[y][x];
            }else{
                return null;
            }
        }
        protected ConnectedComponentLabeling.Component get(ConnectedComponentLabeling.Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    public static abstract class Worker extends Shared implements Callable{
        public final int constant;
        public Worker(ConnectedComponentLabeling.Component[][] array, int lineOrCol) {
            super(array);
            this.constant = lineOrCol;
        }  
    }   
    public static abstract class Linker extends Worker{
        protected int index;
        public Linker(ConnectedComponentLabeling.Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
    }
 
    
    public static class VerticalLinker extends Linker{
        public VerticalLinker(ConnectedComponentLabeling.Component[][] array, int col) {
            super(array, col);
        }
        @Override
        public Object call() throws Exception {
            ConnectedComponentLabeling.Component prev;
            ConnectedComponentLabeling.Component next = this.comp[index][this.constant];
            index++;
            while(index<this.width){
                prev = next;
                next = this.get(index, this.constant);
                if(prev.id == next.id){
                    prev.down = next.location;
                    next.up = prev.location;
                }
                index++;
            }
            return null;
        }

    }
    public static class HorizontalLinker extends Linker{
        public HorizontalLinker(ConnectedComponentLabeling.Component[][] array, int line) {
            super(array, line);
        }
        @Override
        public Object call() throws Exception {
            ConnectedComponentLabeling.Component prev;
            ConnectedComponentLabeling.Component next = this.comp[this.constant][this.index];
            index++;
            while(index<this.length){
                prev = next;
                next = this.get(this.constant, index);
                if(prev.id == next.id){
                    prev.right = next.location;
                    next.left = prev.location;
                }
                index++;
            }
            return null;
        }

    }
    public static Collection<Linker> generateLinkers(ConnectedComponentLabeling.Component[][] array){
        int width = array.length;
        int length = array[0].length;
        ArrayList<Linker> linkers = new ArrayList<>(width+length);
        for(int i=0; i<width; i++){
            linkers.add(new HorizontalLinker(array,i));
        }
        for(int j=0; j<length; j++){
            linkers.add(new VerticalLinker(array,j));
        }
        System.out.println(width +" "+length+" "+linkers.size());
        return linkers;
        
    }
    
    public static AtomicInteger relabelCount = new AtomicInteger(0);
    public static class CompBlock{
        public ConnectedComponentLabeling.Component c1,c2,c3,c4;
        public CompBlock(ConnectedComponentLabeling.Component c1,ConnectedComponentLabeling.Component c2, ConnectedComponentLabeling.Component c3, ConnectedComponentLabeling.Component c4){
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
        }
        public Collection<ConnectedComponentLabeling.Component> getAll(){
            ArrayList<ConnectedComponentLabeling.Component> list = new ArrayList(4);
            list.add(c1);
            list.add(c2);
            list.add(c3);
            list.add(c4);
            return list;
        }
    }
    
     public static Collection<Labeler> generateLabelers(ConnectedComponentLabeling.Component[][] array){
        ArrayList<Labeler> labelers = new ArrayList<>();
        array[0][0].label = getUnusedLabel();
        HorizontalLabeler dependency = null;
        for(int i=0; i<shared.width-1; i++){
            HorizontalLabeler labeler = new HorizontalLabeler(array,i);
            labeler.dependancy = dependency;
            labelers.add(labeler);
            dependency = labeler;
        }
        return labelers;
    }
        public static abstract class Labeler extends Worker{
        public volatile int index;
        public Labeler dependancy;
        public Labeler(ConnectedComponentLabeling.Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
        
    }
    
    public static int test(ConnectedComponentLabeling.Component c1, ConnectedComponentLabeling.Component c2, ConnectedComponentLabeling.Component c3){
        
        if(c1.down!=null){// 1 2 5 7 9
            if(c1.right!=null){// 1 2
                if(c2.down!=null){
                    return 1;
                }else if(c3.right==null){
                    return 2;
                }
            }else{// 5 7 9
                if(c3.right!=null && c2.down == null){
                    return 5;
                }else{
                    if(c2.down != null){
                        return 7;
                    }else{
                        return 9;
                    }
                }
                
            }
        }else{// 3 4 6 8 10 11 12
           if(c1.right!=null){// 3 6 10
                if(c3.right== null){
                    if(c2.down != null){
                        return 3;
                    }else{
                        return 10;
                    }
                }else if(c2.down == null){
                    return 6;
                }
                
            }else{// 4 8 11 12
               
               if(c2.down != null){// 4 11
                   if(c3.right != null){
                       return 4;
                   }else{
                       return 11;
                   }
               }else{// 8 10
                   if(c3.right != null){
                       return 8;
                   }else{
                       return 10;
                   }
               }
                
            } 
        }
        return -1;
        
    }
    public static void labelBlock(CompBlock block, int testCase){
        switch(testCase){
                case 1:{
                    block.c2.label = block.c1.label;
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c1.label;
                    break;
                }
                case 2:{
                    block.c2.label = block.c1.label;
                    block.c3.label = block.c1.label;
//                    if(block.c4.label == null){
                        block.c4.label = getUnusedLabel();
//                    }
                    break;
                }
                case 3:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();
                    }
                    block.c4.label = block.c1.label;
                    break;
                }
                case 4:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();    
                    }
                    
                    block.c3.label = block.c2.label;
                    block.c4.label = block.c2.label;
                    break;
                }
                case 5:{
                    if(block.c2.label == null){
                       block.c2.label = getUnusedLabel(); 
                    }
                    
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c1.label;
                    break;
                }
                case 6:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();
                    }
                    
                    block.c4.label = block.c3.label;
//                    System.out.println(6);
                    break;
                }
                case 7:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c2.label;
                    break;
                }
                case 8:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = getUnusedLabel();
                    block.c4.label = block.c3.label;
//                    System.out.println(8);
                    break;
                }
                case 9:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = block.c1.label;
                    block.c4.label = getUnusedLabel();
//                    System.out.println(9);
                    break;
                }
                case 10:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();  
                    }
                    if(block.c4.label == null){
                        block.c4.label = getUnusedLabel();
                    }
                    
//                    System.out.println(10);
                    break;
                }
                case 11:{
//                    block.c2.label = getUnusedLabel();
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();  
                    }
                    block.c4.label = block.c2.label;
//                    System.out.println(11);
                    break;
                }
                case 12:{
                    block.c2.label = getUnusedLabel();
                    block.c3.label = getUnusedLabel();
                    block.c4.label = getUnusedLabel();
//                    System.out.println(12);
                    break;
                }
        }
    }
    public static class HorizontalLabeler extends Labeler{
        public int consInc;
        public HorizontalLabeler(ConnectedComponentLabeling.Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
            consInc = constant+1;
        }
        @Override
        public Object call() throws Exception {
            ConnectedComponentLabeling.Component c;
            while(index<this.length-1){
                if(this.dependancy !=null){
                    while(index>=dependancy.index){}
                }
                c = get(constant,index);
                
                ConnectedComponentLabeling.Component down = get(consInc,index);
                ConnectedComponentLabeling.Component right = get(constant,index+1);
                ConnectedComponentLabeling.Component downRight = get(consInc,index+1);
                CompBlock block = new CompBlock(c,right,down,downRight);
                int testCase = test(c,right,down);
                c.c = testCase;
                labelBlock(block,testCase);
                //test collision
                // needs recursion
                for(ConnectedComponentLabeling.Component b:block.getAll()){
                    eliminatorUp(b);
                    eliminatorLeft(b); 
                }
                

                
                
                index++;
                
            }
            

            
            index+=100;
            return null;
        }
        
        
        public void eliminatorLeft(ConnectedComponentLabeling.Component start){
            ConnectedComponentLabeling.Component left = get(start.left);
            if(left== null){
                return;
            }
            if((!Objects.equals(start.label, left.label))){
                System.out.println("Collision left "+ start.location.toString());
                ConnectedComponentLabeling.Component next = left;
                String ch = start.label;
                do{
                    next.label = ch;
                    next = get(next.left);
                    if(next == null){
                        break;
                    }
                    eliminatorUp(next);
                    relabelCount.incrementAndGet();
                    
                }while(true);
            }
        }
        public void eliminatorUp(ConnectedComponentLabeling.Component start){
            ConnectedComponentLabeling.Component up = get(start.up);
            if(up != null){
                if((!Objects.equals(start.label, up.label))){
                    System.out.println("Collision up "+ start.location.toString());
                    ConnectedComponentLabeling.Component next = up;
                    ArrayList<ConnectedComponentLabeling.Component> relabelMe = new ArrayList<>();
                    String ch = up.label;
                    do{
                        relabelMe.add(next);
                        
                        if(next.up == null){
                            ch = next.label;
                            break;
                        }
                        next = get(next.up);
                        eliminatorLeft(next);
                        
                        
                        relabelCount.incrementAndGet();
                    }while(true);
                    for(ConnectedComponentLabeling.Component c:relabelMe){
                        c.label = ch;
                    }
                }
            }
        }
        
    }
    
    
    
    public static Collection<Collection<ConnectedComponentLabeling.Component>> massPullOut(Collection<ConnectedComponentLabeling.Component> components){
        ArrayList<Collection<ConnectedComponentLabeling.Component>> all = new ArrayList<>();
        for(ConnectedComponentLabeling.Component c:components){
            Collection<ConnectedComponentLabeling.Component> pullOut = pullOut(c);
            if(!pullOut.isEmpty()){
                all.add(pullOut);
            } 
        }
        return all;
    }
    public static Collection<ConnectedComponentLabeling.Component> pullOut(ConnectedComponentLabeling.Component start){
        
        ArrayList<ConnectedComponentLabeling.Component> list = new ArrayList<>();
        if(start!= null && !start.visited){
            start.visited = true;
            list.add(start);
        }else{
            return list;
        }
        
        list.addAll(pullOut(shared.get(start.up)));
        list.addAll(pullOut(shared.get(start.down)));
        list.addAll(pullOut(shared.get(start.left)));
        list.addAll(pullOut(shared.get(start.right)));
        return list;
    }
    public static void simpleRecursiveStrategy(Shared comp){
        List<ConnectedComponentLabeling.Component[]> list = Arrays.asList(comp.comp);
        ArrayList<ConnectedComponentLabeling.Component> massList = new ArrayList<>();
        for(ConnectedComponentLabeling.Component[] carray:list){
            massList.addAll(Arrays.asList(carray));
        }
        Collection<Collection<ConnectedComponentLabeling.Component>> massPullOut = massPullOut(massList);

        for(Collection<ConnectedComponentLabeling.Component> collection:massPullOut){
            String unusedLabel = getUnusedLabel();
            
            for(ConnectedComponentLabeling.Component c:collection){
                c.label = unusedLabel;
            }
            
        }
    
    }
    
    
    public static void oldStrat(Integer[][] parsePicture) throws Exception{
        ArrayList<Thread> threads = new ArrayList<>();
        ConnectedComponentLabeling.Component[][] array = fromPixelArray(parsePicture);
        tableFunction(parsePicture,print);
        shared = new Shared(array);
        System.out.println();
        Collection<Linker> linkers = generateLinkers(array);
        for(Linker link:linkers){
            link.call();
        }
        long time = System.nanoTime();
        threads.clear();
        System.out.println();
        time = System.currentTimeMillis();
        PullerAPI.advancedStrategy(shared);
//        simpleRecursiveStrategy(shared);        
        
//        System.out.println(System.currentTimeMillis()- time);
        System.out.println();
//        tableFunction(array,printY);
        System.out.println();
//        tableFunction(array,printX);
        tableFunction(array,printLabel);
        System.out.println(shared.get(2,4).up);
        System.exit(0);
    }
    
}
