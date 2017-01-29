/**
 * Created by Kyle on 1/20/2017.
 */
import java.io.*;
import java.util.*;

    public class VPL
    {
        static final int max = 10000;
        static int[] mem = new int[max];
        static int ip, bp, sp, rv, hp, numPassed, gp, k;
        static String fileName;
/*##############################################################################################################*/
        // use symbolic names for all opcodes:
        // op to produce comment on a line by itself
        private static final int noopCode = 0; // ops involved with registers
        private static final int labelCode = 1;
        private static final int callCode = 2;
        private static final int passCode = 3;
        private static final int allocCode = 4;
        private static final int returnCode = 5;  // return a means "return and put
                                                 // copy of value stored in cell a in register rv
        private static final int getRetvalCode = 6;//op a means "copy rv into cell a"
        private static final int jumpCode = 7;
        private static final int condJumpCode = 8;

        // arithmetic ops
        private static final int addCode = 9;
        private static final int subCode = 10;
        private static final int multCode = 11;
        private static final int divCode = 12;
        private static final int remCode = 13;
        private static final int equalCode = 14;
        private static final int notEqualCode = 15;
        private static final int lessCode = 16;
        private static final int lessEqualCode = 17;
        private static final int andCode = 18;
        private static final int orCode = 19;
        private static final int notCode = 20;
        private static final int oppCode = 21;

        // ops involving transfer of data
        private static final int litCode = 22;  // litCode a b means "cell a gets b"
        private static final int copyCode = 23;// copy a b means "cell a gets cell b"
        private static final int getCode = 24; // op a b means "cell a gets
        // contents of cell whose
        // index is stored in b"
        private static final int putCode = 25;  // op a b means "put contents
        // of cell b in cell whose offset is stored in cell a"

        // system-level ops:
        private static final int haltCode = 26;
        private static final int inputCode = 27;
        private static final int outputCode = 28;
        private static final int newlineCode = 29;
        private static final int symbolCode = 30;
        private static final int newCode = 31;

        // global variable ops:
        private static final int allocGlobalCode = 32;
        private static final int toGlobalCode = 33;
        private static final int fromGlobalCode = 34;

        // debug ops:
        private static final int debugCode = 35;

        // return the number of arguments after the opcode,
        // except ops that have a label return number of arguments
        // after the label, which always comes immediately after
        // the opcode
/*##################################################################################################################*/
        public static void main(String[] args) throws Exception
        {
            BufferedReader keys = new BufferedReader(
                    new InputStreamReader( System.in));
            System.out.print("enter name of file containing VPL program: ");
            fileName = keys.readLine();

            // load the program into the front part of
            // memory
            BufferedReader input = new BufferedReader( new FileReader( fileName ));
            String line;
            StringTokenizer st;
            int opcode = -1;

            ArrayList<IntPair> labels, holes;
            labels = new ArrayList<IntPair>();
            holes = new ArrayList<IntPair>();
            int label;

            k=0;
            do {
                line = input.readLine();
                System.out.println("parsing line [" + line + "]");
                if( line != null )
                {// extract any tokens
                    st = new StringTokenizer( line );
                    if( st.countTokens() > 0 )
                    {// have a token, so must be an instruction (as opposed to empty line)

                        opcode = Integer.parseInt(st.nextToken());

                        // load the instruction into memory:

                        if( opcode == labelCode )
                        {// note index that comes where label would go
                            label = Integer.parseInt(st.nextToken());
                            labels.add( new IntPair( label, k ) );
                        }
                        else
                        {// opcode actually gets stored
                            mem[k] = opcode;  ++k;

                            if( opcode == callCode || opcode == jumpCode ||
                                    opcode == condJumpCode )
                            {// note the hole immediately after the opcode to be filled in later
                                label = Integer.parseInt( st.nextToken() );
                                mem[k] = label;  holes.add( new IntPair( k, label ) );
                                ++k;
                            }

                            // load correct number of arguments (following label, if any):
                            for( int j=0; j<numArgs(opcode); ++j )
                            {
                                mem[k] = Integer.parseInt(st.nextToken());
                                ++k;
                            }

                        }// not a label

                    }// have a token, so must be an instruction
                }// have a line
            }while( line != null );

            //System.out.println("after first scan:");
            //showMem( 0, k-1 );

            // fill in all the holes:
            int index;
            for( int m=0; m<holes.size(); ++m )
            {
                label = holes.get(m).second;
                index = -1;
                for( int n=0; n<labels.size(); ++n )
                    if( labels.get(n).first == label )
                        index = labels.get(n).second;
                mem[ holes.get(m).first ] = index;
            }

            System.out.println("after replacing labels:");
            showMem( 0, k-1 );

            // initialize registers:
            bp = k;  sp = k+2;  ip = 0;  rv = -1;  hp = max;
            numPassed = 0;

            int codeEnd = bp-1;

            System.out.println("Code is " );
            showMem( 0, codeEnd );

            gp = codeEnd + 1;
            run();
        }// main

        private static void run(){
            int nParams =0; //this indicates the number of parameters being passed into the subroutine
            int push = 2; //keeps track of the top of the new stack before moving sp
            Scanner input = new Scanner(System.in);
            int opcode;
            while(ip<k) {
                opcode = mem[ip];
                switch (opcode) {
                    case 0://noopCode:              //0//
                        ip++;
                        break;

                    case 1: //labelCode:             //1//
                        //Do nothing
                        ip += 2; //move ip forward to next instruction. This should never happen, but in case it does the program will continue
                        break;

                    case 2: //callCode:              //2//
                        mem[sp] = bp; //puts the previous bp into index for returning from subroutine
                        mem[sp + 1] = ip + 2; //puts the instruction after the call into stack frame for returning from subroutine
                        bp = sp; //increase the bp to the beginning of the new stack frame
                        sp = 2 + nParams; //Move the sp to the end of the stack frame
                        push = 2; //reset push for next call
                        ip = mem[ip + 1]; //Move the ip to the first instruction of the new routine marked by the label
                        showStackFrame();
                        break;

                    case 3: //passCode:              //3//
                        mem[sp + push++] = mem[bp + 2 + mem[ip + 1]]; //this PUSHes the contents of a onto the upcoming stack frame
                        ip += 2;
                        break;

                    case 4: //allocCode:             //4//
                        sp += mem[ip + 1]; //increases sp by n (ip+1) for local variables
                        ip += 2;
                        break;

                    case 5: //returnCode:            //5//
                        rv = mem[bp + 2 + mem[ip + 1]]; //loads the contents of the given cell into rv for return
                        ip = mem[bp+1]; //Moves ip to next instruction in the parent routine
                        sp = bp; //moves sp back to top of parent stack frame
                        bp = mem[bp]; //Moves bp back to base of parent stack frame
                        showStackFrame();
                        break;

                    case getRetvalCode:         //6//
                        mem[bp + 2 + mem[ip + 1]] = rv; //Puts returned value into ath cell in current stack frame
                        ip += 2;
                        showStackFrame();
                        break;

                    case jumpCode:              //7//
                        ip = mem[ip + 1]; //jump to location in ip+1
                        break;

                    case condJumpCode:          //8//
                        if (mem[bp + 2 + mem[ip + 2]] == 0) {
                            //if the contents of the given cell is 0 the branch condition fails and ip moves to the next sequential instruction
                            ip += 3;
                        } else {
                            //otherwise if the contents are non-zero, then the branch is taken and ip moves to location in ip+1
                            ip = mem[ip + 1];
                        }
                        break;

                    case addCode:               //9//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] + mem[bp + 2 + mem[ip + 3]]; //a = b + c
                        ip += 4;
                        break;

                    case subCode:              //10//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] - mem[bp + 2 + mem[ip + 3]]; //a = b - c
                        ip += 4;
                        break;

                    case multCode:             //11//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] * mem[bp + 2 + mem[ip + 3]]; //a = b * c
                        ip += 4;
                        break;

                    case divCode:              //12//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] / mem[bp + 2 + mem[ip + 3]]; //a = b / c
                        ip += 4;
                        break;

                    case remCode:              //13//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] % mem[bp + 2 + mem[ip + 3]]; //a = b % c
                        ip += 4;
                        break;

                    case equalCode:            //14//
                        if (mem[bp + 2 + mem[ip + 2]] - mem[bp + 2 + mem[ip + 3]] == 0) { //checks to see if b == c by b-c and checking for zero
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if zero then they are equal and the boolean value 1 (true) is saved in a
                        } else { //if the difference isn't zero
                            mem[bp + 2 + mem[ip + 1]] = 0; //not equal and the boolean value 0 (false) is saved in a
                        }
                        ip += 4;
                        break;

                    case notEqualCode:         //15//
                        if (mem[bp + 2 + mem[ip + 2]] - mem[bp + 2 + mem[ip + 3]] != 0) { //checks to see if b != c by b-c and checking for zero
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if not zero then they aren't equal and the boolean value 1 (true) is saved in cell a
                        } else { //if the difference is zero
                            mem[bp + 2 + mem[ip + 1]] = 0; //b and c are equal and the boolean value 0 (false) is saved in cell a
                        }
                        ip += 4;
                        break;

                    case lessCode:             //16//
                        if (mem[bp + 2 + mem[ip + 2]] - mem[bp + 2 + mem[ip + 3]] < 0) { //checks to see if b < c by b-c and checking for a negative
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if below zero then b is less than c and the boolean value 1 (true) is saved in cell a
                        } else { //if the difference isn't below zero
                            mem[bp + 2 + mem[ip + 1]] = 0; //not less than and the boolean value 0 (false) is saved in cell a
                        }
                        ip += 4;
                        break;

                    case lessEqualCode:        //17//
                        if (mem[bp + 2 + mem[ip + 2]] - mem[bp + 2 + mem[ip + 3]] <= 0) { //checks to see if b <= c by b-c and checking for a negative or zero
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if zero or less, then b is less than or equal to c and the boolean value 1 (true) is saved in cell a
                        } else { //if the difference isn't zero or less
                            mem[bp + 2 + mem[ip + 1]] = 0; //not less than or equal and the boolean value 0 (false) is saved in cell a
                        }
                        ip += 4;
                        break;

                    case andCode:              //18//
                        if (mem[bp + 2 + mem[ip + 2]] != 0 && mem[bp + 2 + mem[ip + 3]] != 0) { //checks to see if b AND c are not zero
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if both are non-zero than the boolean value 1 (true) is saved in cell a
                        } else { //if either b or c contains zero
                            mem[bp + 2 + mem[ip + 1]] = 0; //the boolean value 0 (false) is saved in cell a
                        }
                        ip += 4;
                        break;

                    case orCode:               //19//
                        if (mem[bp + 2 + mem[ip + 2]] != 0 || mem[bp + 2 + mem[ip + 3]] != 0) { //checks to see if b OR c are not zero
                            mem[bp + 2 + mem[ip + 1]] = 1;  //if at least one is non-zero than the boolean value 1 (true) is saved in cell a
                        } else { //if both b and c contain zero
                            mem[bp + 2 + mem[ip + 1]] = 0; //the boolean value 0 (false) is saved in cell a
                        }
                        ip += 4;
                        break;

                    case notCode:              //20//
                        if (mem[bp + 2 + mem[ip + 2]] == 0) {
                            mem[bp + 2 + 1] = 1; //put the opposite boolean value of cell b into cell a
                        } else {
                            mem[bp + 2 + mem[ip + 1]] = 0; //put the opposite boolean value of cell b into cell a
                        }
                        ip += 3;
                        break;

                    case oppCode:              //21//
                        //sub the value in cell b by twice its value to get the opposite sign of the same abs value
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]] - (2 * mem[bp + 2 + mem[ip + 2]]);
                        ip += 3;
                        break;

                    case litCode:              //22//
                        mem[bp + 2 + mem[ip + 1]] = mem[ip + 2]; //puts the literal in cell b into virtual stack position in cell a
                        ip += 3;
                        break;

                    case copyCode:             //23//
                        mem[bp + 2 + mem[ip + 1]] = mem[bp + 2 + mem[ip + 2]]; //copies the value from cell b into cell a
                        ip += 3;
                        break;

                    case getCode:              //24//
                        //TODO
                        ip += 4;
                        break;

                    case putCode:              //25//
                        //TODO
                        ip += 4;
                        break;

                    case haltCode:             //26//
                        System.exit(0);
                        break;

                    case inputCode:            //27//
                        System.out.print("? ");//prompt for user input
                        int in = input.nextInt(); //takes user input
                        mem[bp + 2 + mem[ip + 1]] = in; //puts user input into cell a
                        ip += 2;
                        break;

                    case outputCode:               //28//
                        System.out.print("> " + mem[bp + 2 + mem[ip + 1]]); //displays the contents of cell a
                        ip += 2;
                        break;

                    case newlineCode:              //29//
                        System.out.println(); //move cursor to next line
                        ip += 1;
                        break;

                    case symbolCode:               //30//
                        //TODO
                        ip += 2;
                        break;

                    case newCode:                  //31//
                        //TODO
                        ip += 3;
                        break;

                    case allocGlobalCode:          //32//
                        //TODO
                        ip += 2;
                        break;

                    case toGlobalCode:             //33//
                        //TODO
                        ip += 3;
                        break;

                    case fromGlobalCode:           //34//
                        //TODO
                        ip += 3;
                        break;

                    case debugCode:                //35//
                        //TODO
                        break;

                    default:
                        System.out.println("Something went wrong!");
                }
            }

        }//run

        private static int numArgs( int opcode ) {
            // highlight specially behaving operations
            if( opcode == labelCode ) return 1;  // not used
            else if( opcode == jumpCode ) return 0;  // jump label
            else if( opcode == condJumpCode ) return 1;  // condJump label expr
            else if( opcode == callCode ) return 0;  // call label

                // for all other ops, lump by count:

            else if( opcode==noopCode ||
                    opcode==haltCode ||
                    opcode==newlineCode ||
                    opcode==debugCode
                    )
                return 0;  // op

            else if( opcode==passCode || opcode==allocCode ||
                    opcode==returnCode || opcode==getRetvalCode ||
                    opcode==inputCode ||
                    opcode==outputCode || opcode==symbolCode ||
                    opcode==allocGlobalCode
                    )
                return 1;  // op arg1

            else if( opcode==notCode || opcode==oppCode ||
                    opcode==litCode || opcode==copyCode || opcode==newCode ||
                    opcode==toGlobalCode || opcode==fromGlobalCode

                    )
                return 2;  // op arg1 arg2

            else if( opcode==addCode ||  opcode==subCode || opcode==multCode ||
                    opcode==divCode ||  opcode==remCode || opcode==equalCode ||
                    opcode==notEqualCode ||  opcode==lessCode ||
                    opcode==lessEqualCode || opcode==andCode ||
                    opcode==orCode || opcode==getCode || opcode==putCode
                    )
                return 3;

            else
            {
                System.out.println("Fatal error: unknown opcode [" + opcode + "]" );
                System.exit(1);
                return -1;
            }

        }// numArgs

        private static void showMem( int a, int b ) {
            for( int k=a; k<=b; ++k )
            {
                System.out.println( k + ": " + mem[k] );
            }
        }// showMem

        private static void showStackFrame(){
            //For debugging purposes. Display everything from the bp to the sp//
            System.out.print("BP[ ");
            for(int i = bp;i<sp;i++){
                if(i < sp-1) {
                    System.out.print(mem[i] + ", ");
                }
                else {
                    System.out.print(mem[i]);
                }
            }
            System.out.print(" ]SP\n");
        }//showStackFrame

    }// VPLstart
