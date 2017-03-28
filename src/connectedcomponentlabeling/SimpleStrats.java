/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

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
        public Component[][] comp;
        public final int length,width;
        public Shared(Component[][] array){
            this.comp = array;
            this.width = array.length;
            this.length = array[0].length;
        }
        protected Component get(int y, int x){
            if(y<this.width && x<this.length){
                return this.comp[y][x];
            }else{
                return null;
            }
        }
        protected Component get(ConnectedComponentLabeling.Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    public static class Component extends ConnectedComponentLabeling.MiniComponent{
        public ConnectedComponentLabeling.Pos up;
        public ConnectedComponentLabeling.Pos left;
        public ConnectedComponentLabeling.Pos right;
        public int c;
        public volatile boolean visited;
        
        public Component(int Y, int X, int id){
            super(Y,X,id);
            this.c = 0;
        }
        @Override
        public String toString(){
            
            return String.valueOf(this.location);
        }
        
    }
    
    
    
    public static abstract class Worker extends Shared implements Callable{
        public final int constant;
        public Worker(Component[][] array, int lineOrCol) {
            super(array);
            this.constant = lineOrCol;
        }  
    }   
    public static abstract class Linker extends Worker{
        protected int index;
        public Linker(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
    }
 
    
    public static class VerticalLinker extends Linker{
        public VerticalLinker(Component[][] array, int col) {
            super(array, col);
        }
        @Override
        public Object call() throws Exception {
            Component prev;
            Component next = this.comp[index][this.constant];
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
        public HorizontalLinker(Component[][] array, int line) {
            super(array, line);
        }
        @Override
        public Object call() throws Exception {
            Component prev;
            Component next = this.comp[this.constant][this.index];
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
    public static Collection<Linker> generateLinkers(Component[][] array){
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
        public Component c1,c2,c3,c4;
        public CompBlock(Component c1,Component c2, Component c3, Component c4){
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
        }
        public Collection<Component> getAll(){
            ArrayList<Component> list = new ArrayList(4);
            list.add(c1);
            list.add(c2);
            list.add(c3);
            list.add(c4);
            return list;
        }
    }
    
    public static Collection<Labeler> generateLabelers(Component[][] array){
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
        public Labeler(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
        
    }
    
    public static int test(Component c1, Component c2, Component c3){
        
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
        public HorizontalLabeler(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
            consInc = constant+1;
        }
        @Override
        public Object call() throws Exception {
            Component c;
            while(index<this.length-1){
                if(this.dependancy !=null){
                    while(index>=dependancy.index){}
                }
                c = get(constant,index);
                
                Component down = get(consInc,index);
                Component right = get(constant,index+1);
                Component downRight = get(consInc,index+1);
                CompBlock block = new CompBlock(c,right,down,downRight);
                int testCase = test(c,right,down);
                c.c = testCase;
                labelBlock(block,testCase);
                //test collision
                // needs recursion
                for(Component b:block.getAll()){
                    eliminatorUp(b);
                    eliminatorLeft(b); 
                }
                

                
                
                index++;
                
            }
            

            
            index+=100;
            return null;
        }
        
        
        public void eliminatorLeft(Component start){
            Component left = get(start.left);
            if(left== null){
                return;
            }
            if((!Objects.equals(start.label, left.label))){
                System.out.println("Collision left "+ start.location.toString());
                Component next = left;
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
        public void eliminatorUp(Component start){
            Component up = get(start.up);
            if(up != null){
                if((!Objects.equals(start.label, up.label))){
                    System.out.println("Collision up "+ start.location.toString());
                    Component next = up;
                    ArrayList<Component> relabelMe = new ArrayList<>();
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
                    for(Component c:relabelMe){
                        c.label = ch;
                    }
                }
            }
        }
        
    }
    
    
     public static Component[][] fromPixelArray(Integer[][] pixels){
        int width = pixels.length;
        int length = pixels[0].length;
        Component[][] array = new Component[width][length];
        for(int i=0;i<width;i++){
            for(int j=0; j<length; j++){
                Component comp = new Component(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
    
    public static Collection<Collection<Component>> massPullOut(Collection<Component> components){
        ArrayList<Collection<Component>> all = new ArrayList<>();
        for(Component c:components){
            Collection<Component> pullOut = pullOut(c);
            if(!pullOut.isEmpty()){
                all.add(pullOut);
            } 
        }
        return all;
    }
    public static Collection<Component> pullOut(Component start){
        
        ArrayList<Component> list = new ArrayList<>();
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
        List<Component[]> list = Arrays.asList(comp.comp);
        ArrayList<Component> massList = new ArrayList<>();
        for(Component[] carray:list){
            massList.addAll(Arrays.asList(carray));
        }
        Collection<Collection<Component>> massPullOut = massPullOut(massList);

        for(Collection<Component> collection:massPullOut){
            String unusedLabel = getUnusedLabel();
            
            for(Component c:collection){
                c.label = unusedLabel;
            }
            
        }
    
    }
    
    
    public static void oldStrat(Integer[][] parsePicture) throws Exception{
        ArrayList<Thread> threads = new ArrayList<>();
        Component[][] array = fromPixelArray(parsePicture);
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
    
    
    
    public static ConnectedComponentLabeling.iTableFunction printCase = new ConnectedComponentLabeling.iTableFunction() {
        @Override
        public void onIteration(Object[][] array, int x, int y) {
            Component comp = (Component)array[x][y];
            String printme = " ";
            if(comp != null){
                printme = comp.c + "";
            }
            System.out.print(printme+" ");
        }

        @Override
        public void onNewArray(Object[][] array, int x, int y) {
            System.out.println(" :"+x);
        }
    };
}
