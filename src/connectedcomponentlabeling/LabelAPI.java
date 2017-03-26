/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import connectedcomponentlabeling.ConnectedComponentLabeling.Component;
import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.relabelCount;
import static connectedcomponentlabeling.ConnectedComponentLabeling.shared;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 *
 * @author Lemmin
 */
public class LabelAPI {
    
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
         public static abstract class Labeler extends ConnectedComponentLabeling.Worker{
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
            ConnectedComponentLabeling.Component c;
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
    
}
