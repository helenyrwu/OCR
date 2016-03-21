/*
 * NeuralNets_05_13_15
 * 
 * @author Helen Wu
 * @version 05/13/15
 * 
 * 
 * This file contains the NeuralNets class and the main method for training
 * and running the optical character recognition assignment with back propagation. 
 * Comments in this file will make use of terminology, methodology, and equations 
 * found in Dr. Nelson's notes. This program is able to train neural networks
 * with three layers (more on the structure in the NeuralNets class top comment below). 
 * I trained the network to return a 6-digit binary number from 1 to 52 depending 
 * on which character was input. The input for each test case consists of an array 
 * of the last 8 bits of each pixel element from the bitmap, which is acquired by 
 * putting a bitmap of a character through BitmapDump.java. The "A" bitmap will 
 * yield "0 0 0 0 0 1", "B" will yield 0 0 0 0 1 0,..., "y" will yield "1 1 0 0 1 1", 
 * and "z" will yield "1 1 0 1 0 0".
 * 
 * Any output below 0.25 is considered 0, and any output above 0.75 is considered 1.
 * Any outputs between 0.25 and 0.75 indicate an unfamiliar character.
 * 
 * Because the test cases are read in from a file, this code is flexible, 
 * and the network can be trained to output whatever array of numbers
 * the user desires (more than six outputs and more than 0 and 1).
 * 
 * 
 * To use this file, the user should run the main method. 
 * 
 * If the user wants to train the network according to some test cases,
 * the user should create a file with test cases called "OCR_trainingset.in" in the 
 * same directory as this file. Then, the user should run the main method and type 
 * "1" when prompted by the program.
 * 
 *    Format of "OCR_trainingset.in": The first line of testFile, in which the test 
 *    case inputs and outputs are stored, is the total number of test cases (models) 
 *    given. 
 *    After the first line, subsequent lines consist of one line of inputs in a 
 *    test case  and then one line of the corresponding test case outputs. 
 *    The input and output lines alternate until all the test cases are 
 *    enumerated in the file.
 * 
 * If the user wants to run the network with existing weights and
 * given inputs, the user should create a file called "OCR_inputset.in" 
 * with inputs and a file with weights called "weights.in" in the 
 * same directory as this file. Then, the user should run the main method
 * and type "2" (in reality, any number other than "1" will work)
 * when prompted by the program.
 *    
 *    Format of "OCR_inputset.in": Lines consisting of the desired inputs (representing
 *                           the bitmap of a certain character); each line is 
 *                           corresponds to one bitmap file/character
 *    
 *    Format of "weights.in": First line consists of k-j weights
 *                            Second line consists of j-i weights
 * 
 * 
 * The final weights and outputs will be printed on the screen. If the
 * program is training, the program will print the reason for ending
 * the program (the total error is less than some minimum error you
 * desire to reach or the number of iterations is greater than some
 * maximum number of iterations). The outputs will also be printed
 * out to a file called "output.out". The weights are printed to a file
 * named "weights.out".
 * 
 */

import java.io.*;
import java.util.*;

/*
 * This class contains a neural network with three layers -- input, hidden, and output
 * layers. The input layer has NUM_INPUTS nodes, the hidden layer has NUM_HIDDEN nodes, 
 * and the output layer has NUM_OUTPUTS nodes. When the neural network is being run,
 * the values of the nodes other than the input nodes (the values of which are given)
 * are determined according to the method described in Dr. Nelson's notes. 
 * 
 * First, the propagation rule, found in the notes, is applied using the inputs and 
 * k-j weights. It essentially multiplies the appropriate inputs and weights and 
 * adds the products together.
 * 
 * Propagation rule: propagation[j] = inputs[1]*w1j + inputs[2]*w2j + .. inputs[i]*wij
 * 
 * Then, the activation rule is applied to find the value of the nodes.
 * 
 * Activation rule: hidden[j] = 1.0 / (1.0 + Math.exp(-propagation[j]))
 * 
 * The same process (propagation rule followed by the activation rule) 
 * is used to find the output values from the hidden node values.
 * 
 * If the neural network is being trained, as outlined in the notes,
 * the weights are adjusted according to the steepest descent method.
 * The derivatives of the errors with respect to the weights are found,
 * the weights are changed by 
 * 
 *       deltaWeight = -learningFactor * (derivative of the error)
 * 
 * The process is repeated using the new weights until the error is
 * sufficiently small or the number of counts has exceeded MAX_COUNT.
 * 
 * This program uses the backpropagation algorithm, which optimizes the process
 * outlined above at the expense of readability. I have written this program according
 * to the formulae and concepts delineated in Dr. Nelson's notes "Minimizing and
 * Optimizing the Error Function."
 * 
 * I have left some of the methods that were used by the steepest descent method even
 * though they are not used in back propagation.
 * 
 * --------------------------------------------------------------------
 * 
 * Overview of methods in this class:
 * 
 * public static void main(String[] args) throws IOException
 *       Runs the program -- allows user to select whether the program
 *       is running (to produce outputs for given inputs) or training
 * 
 * public NeuralNets()
 *       Constructor that initialilzes instance variables and
 *       allows the user to decide whether to train or run
 *       the neural network
 *       
 * public void train(double totalError) throws IOException  
 *       Contains all the necessary steps to train the network
 *       to run and mimick the test cases given
 *       
 * public void forward()
 *       Contains the forward loop for the back propagation algorithm
 *       
 * public void backward()
 *       Contains the backward loop for the back propagation algorithm
 *       
 * public void randomizeWeights(double[] weightArray, int numElements)
 *       Sets all elements in weightArray to random values between -1 and 1
 *       
 * public void setWeights() throws IOException
 *       Sets weights to random if training and to weights from file if running
 *       
 * public void readWeights(BufferedReader fWeights, double[] weightArray, 
 *                         int numElements) throws IOException
 *       Reads and sets weight array to weights from file
 *       
 * public void setTestCases(BufferedReader fTest) throws IOException
 *       Sets input and output arrays for the current test case from file
 *       
 * public void setInputs()
 *       Sets inputs to test case inputs
 *       
 * public double findError()
 *       Finds and returns error between outputs and test case outputs
 * 
 * public void setNewWeights(double[] weightsArray, double[] errorDerivs, 
 *                           double learningFactor)
 *       Sets new weights based on derivatives of error and learning factor
 *                             
 * public void printResults(double totalError) throws IOException
 *       Prints results of the program: outputs, weights, etc.
 * 
 * public void runWithInputs() throws IOException
 *       Runs the program (as opposed to training the network) with inputs
 *       read in from text files
 * 
 * public double[] findOutput() 
 *       Calculates and returns array with the output values
 * 
 * public double activationFunction(double propagation)
 *       Performs the activation function on propagation
 *       and returns the result
 * 
 * public double functionDeriv(double funcOutput)
 *       Calculates and returns the derivative of the function
 * 
 * public void findDerivs()
 *       Calculates the derivative of the errors with respect to the weights
 */
public class NeuralNets
{
   public static final int NUM_INPUTS = 10201;
   public static final int NUM_HIDDEN = 100;
   public static final int NUM_OUTPUTS = 6;
   public static final int MAX_COUNT = 10000000;
   public static final double MIN_ERROR = 1.0;
   public static final double MAX_WEIGHT = 1.0;
   public static final double INITIAL_LEARNING_FACTOR = .000025;
   public static final double INITIAL_ERROR = 60.0;             //for initialization purposes
   public static final double CASES_PER_PRINT = 1;
   public static final double LEARNING_MULTIPLIER_KJ = .5;      //used to set the 
   public static final double LEARNING_MULTIPLIER_JI = .5;      //learning factor
   
   
   int numElementskj, numElementsji;
   double[] weightskj, weightsji, inputs, hidden, outputs, testInput, 
            testOutput, errorDerivskj, errorDerivsji, omega_i, omega_j, 
            psi_i, psi_j, theta_j, theta_i;
   double learningFactorkj, learningFactorji, prevError;
   int shouldTrain;
   
   
   /*
    * If the main method is used to train the network (when net.shouldTrain == 1), 
    * given test cases are used as models to train the network (find certain weights) 
    * to output the expected results. 
    * The weights are initially randomized between -1 and 1, and the outputs 
    * for the test case inputs are found. Using the steepest descent algorithm, 
    * the error is minimized until the error is less than MIN_ERROR or 
    * the number of iterations is greater than MAX_COUNT.
    * 
    * When the NeuralNets class is used to run with given inputs, the inputs 
    * and weights are read from files into arrays and the outputs are found.
    * 
    * Then, the results are printed and saved to a file (more info on 
    * this in printResults() method documentation).
    * 
    * totalError is initialized with an arbitrarily large number INITIAL_ERROR
    * to ensure that the while loop in net.train() can be entered.
    */
   public static void main(String[] args) throws IOException
   {
      double totalError = INITIAL_ERROR;     

      NeuralNets net = new NeuralNets();
      
      if (net.shouldTrain == 1)
      {
         totalError = net.train(totalError);
      }
      else
      {
         net.runWithInputs();
      }
      
      net.printResults(totalError);
      
      return;
   }
   
   /*
    * Constructor for  objects of class NeuralNets: This constuctor initializes 
    * all the instance variables. The learning factors are initialized to 
    * INITIAL_LEARNING_FACTOR for now, but within the program, they will be 
    * dependent on error. The constructor also takes an input from the user 
    * saved in the variable shouldTrain that will determine whether the program 
    * trains (if the user inputs 1) or runs with existing weights 
    * (if the user inputs any other number).
    * 
    *    inputs         array of input nodes
    *    hidden         array of hidden nodes
    *    outputs        array of output nodes
    *    testInput      array of input values for the current test case
    *    testOutput     array of output values for the current test case
    *    errorDerivskj  array of derivatives of the error with respect to kj weights
    *    errorDerivsji  array of derivatives of the error with respect to ji weights
    */
   public NeuralNets()
   {
      numElementskj = NUM_INPUTS*NUM_HIDDEN;             //number of k-j weights
      numElementsji = NUM_HIDDEN*NUM_OUTPUTS;            //number of j-i weights
      weightskj= new double[numElementskj];
      weightsji= new double[numElementsji];
      inputs = new double[NUM_INPUTS];
      hidden = new double[NUM_HIDDEN];
      outputs = new double[NUM_OUTPUTS];
      testInput = new double[NUM_INPUTS];
      testOutput = new double[NUM_OUTPUTS];
      errorDerivskj = new double[numElementskj];         //error derivatives
      errorDerivsji = new double[numElementsji];
      learningFactorkj = INITIAL_LEARNING_FACTOR;        //initializing learning factors                  
      learningFactorji = INITIAL_LEARNING_FACTOR; 
      prevError = 0;
      theta_i = new double[NUM_OUTPUTS];
      theta_j = new double[NUM_HIDDEN];
      omega_i = new double[NUM_OUTPUTS];
      omega_j = new double[NUM_HIDDEN];
      psi_i = new double[NUM_OUTPUTS];
      psi_j = new double[NUM_HIDDEN];
      
      
      Scanner in = new Scanner(System.in);
      
      System.out.println("Type 1 to find weights (train) " +
                         "or any other number to run with saved weights.");
      
      shouldTrain = in.nextInt();                        
   }
      
   
   
   
   /*
    * This method calls a method to set the weights (random if training, 
    * from file if not) runs the network. A BufferedReader is used to read in 
    * the test cases from file with the name testFile.
    * 
    * Format of testFile: The first line of testFile, in which the test case 
    * inputs and outputs are stored, is the total number of test cases (models) given. 
    * After the first line, subsequent lines consist of one line of inputs in a 
    * test case  and then one line of the corresponding test case outputs. 
    * The input and output lines alternate until all the test cases are in the file.
    * 
    * A while loop is used to keep the program running until the total error is 
    * smaller than a given MIN_ERROR or the number of times the for loop is run 
    * (count) is greater than a given MAX_COUNT. One iteration of the while loop 
    * runs through all of the test cases. The total error is calculated
    * over each iteration of the while loop by adding together the individual
    * errors found in each test case within the while loop iteration. The totalError 
    * is reset to 0 each iteration of the while loop to prevent accumulation of error. 
    * 
    * The inputs and outputs of each test case are read into the program as 
    * each test case runs. Therefore, to reset when all the test cases have 
    * been read through, a new BufferedReader is created for the test case 
    * file every time all the test cases have been run through in order to
    * reset and start from the first test case again.
    * 
    * Each iteration over the for loop is training using one test case.
    * 
    * The back propagation algorithm has a loop forward and backward for 
    * each test case that runs the training. The loops were written according
    * to Dr. Nelson's notes on back propagation.
    * 
    * The total error is printed out every CASES_PER_PRINT = 1 case 
    * in order to track progress of the program.
    * 
    * The error and totalError are set initially INITIAL_ERROR so that 
    * the while loop is allowed to run.
    * 
    * The learning factor is originally set to .000025 and is reduced by a factor 
    * of 2.0 if the current total error is greater than or equal to the previous 
    * total error.
    * 
    * 
    * 
    * BufferedReader f2 - BufferedReader for the file containing the test cases.
    * double error      - error for each test case 
    *                     (updated each time one test case is run)
    * 
    * @param totalError   the total error for each full iteration of training 
    *                     (updated each time the test cases are run through 
    *                     completely one time)
    *                     
    * @param              the total error
    */
   public double train(double totalError) throws IOException
   {
      setWeights();
      
      double error = INITIAL_ERROR;          
      int count = 0;
      String testFile = "OCR_trainingset.in";          //name of file where test cases 
                                                       //are located
      
      
      BufferedReader f2 = new BufferedReader(new FileReader(testFile));
      
                              
      int numModels = Integer.parseInt(f2.readLine()); //read in number of test cases
      
      
                               
      while (totalError > MIN_ERROR && count <= MAX_COUNT)
      {
         totalError = 0;                               //resetting total error
         
        
         for (int modelCount = 0; modelCount < numModels; modelCount++)
         {
            if (count % numModels == 0 && count != 0)  //resets to first test case
            {
               f2 = new BufferedReader(new FileReader(testFile));
               f2.readLine();
            }
               
            
            setTestCases(f2);
            setInputs();
            
     
            forward();           //forward loop in back propagation
            backward();          //backward loop in back propagation
            
           
            error = findError(); //calculate error for each test case to decide 
                                 //whether the program should stop or continue
               
            totalError += error;
            count++;
         } // for (int modelCount = 0; modelCount<numModels; modelCount++)
         
         
         if (count % CASES_PER_PRINT == 0)
         {
            System.out.printf("TOTAL ERROR: %f\n\n",totalError);
         }
         
         /*
          * Initialization of prevError so that learning factor will
          * not decrease in first iteration
          */
         if (count == numModels)     
         {
            prevError = totalError;
         }
         
         /*
          * Adjusts learning factor if the current total error is 
          * greater than or equal to the previous error
          */
         if (totalError >= prevError) 
         {
            learningFactorkj = learningFactorkj / 2.0;
            learningFactorji = learningFactorji / 2.0;
         }
         
         prevError = totalError;
               
      }    // while(totalError>MIN_ERROR && count<=MAX_COUNT)
      
      return totalError;
   }       // public double train(double totalError) throws IOException
   
   /*
    * The forward loop of the back propagation is written according to the concepts and
    * formulae given in Dr. Nelson's notes.
    * 
    *    theta_i[i] = h1*w1i + h2*w2i + ... + hn*wni
    *    theta_j[j] = a1*w1j + a2*w2j + ... + hm*wmj
    *    omega_i[i] = expected output - calculated output 
    *    
    * Note: the weight indices (indkj, indji) are set differently
    * because the weight arrays are arranged according to this specification:
    * 
    *       kj weights:   w11 w12 ... w1n; w21 w22 ... w2n; w31...
    * 
    * where the first number is the input layer index and 
    *       the second number is the hidden layer index and
    *       n is the number of hidden layers
    * 
    *       ji weights:   w11 w12 ... w1m; w21 w22 ... w2m; w31...
    * 
    * where the first number is the hidden layer index and 
    *       the second number is the output layer index and
    *       m is the number of output layers
    *       
    * Therefore, to get to the desired weight in the weight array,
    * the index must be (k * NUM_HIDDEN + j) for kj weights.
    *    
    */
   public void forward()
   {
      for (int i = 0; i < NUM_OUTPUTS; i++)
      {
         int indji = 0;
         
         theta_i[i] = 0;    // reset theta_i[i]
         
         for (int j = 0; j < NUM_HIDDEN; j++)
         {
            int indkj = 0;
            
            theta_j[j] = 0; // reset theta_j[j]
            
            for (int k = 0; k < NUM_INPUTS; k++)
            {
               indkj = k * NUM_HIDDEN + j;
               theta_j[j] += inputs[k] * weightskj[indkj];
            }
            
            hidden[j] = activationFunction(theta_j[j]);
            
            indji = j * NUM_OUTPUTS + i;
            theta_i[i] += weightsji[indji] * hidden[j];
            
         } // for (int j = 0; j < NUM_HIDDEN; j++)
         
         outputs[i] = activationFunction(theta_i[i]);
         
         omega_i[i] = testOutput[i] - outputs[i];
         
      }    // for (int i = 0; i < NUM_INPUTS; i++)
      
   }       // public void forward()
   
   /*
    * The backward loop of the back propagation is written according to the concepts and
    * formulae given in Dr. Nelson's notes.
    * 
    *    omega_j[j] = psi[0] * weightsji[indj0] + psi[1] * weightsji[indj1] + ... +
    *                 psi[n] * weightsji[indjn]
    *    psi_j[j] = omega[j] * f'(theta_j[j])
    *    
    * Note: the weight indices (indkj, indji) are set differently
    * because the weight arrays are arranged according to this specification:
    * 
    *       kj weights:   w11 w12 ... w1n; w21 w22 ... w2n; w31...
    * 
    * where the first number is the input layer index and 
    *       the second number is the hidden layer index and
    *       n is the number of hidden layers
    * 
    *       ji weights:   w11 w12 ... w1m; w21 w22 ... w2m; w31...
    * 
    * where the first number is the hidden layer index and 
    *       the second number is the output layer index and
    *       m is the number of output layers
    *       
    * Therefore, to get to the desired weight in the weight array,
    * the index must be (k * NUM_HIDDEN + j) for kj weights.
    */
   public void backward()
   {
      for (int k = 0; k < NUM_INPUTS; k++)
      {
         int indkj = 0;
         
         for (int j = 0; j < NUM_HIDDEN; j++)
         {
            int indji = 0;
            
            omega_j[j] = 0; // reset
            
            for (int i = 0; i < NUM_OUTPUTS; i++)
            {
               psi_i[i] = omega_i[i] * functionDeriv(outputs[i]);
               
               indji = j * NUM_OUTPUTS+ i;
               omega_j[j] += psi_i[i] * weightsji[indji];
               
               weightsji[indji] += learningFactorji * hidden[j] * psi_i[i];
            } // for (int i = 0; i < NUM_OUTPUTS; i++)
            
            psi_j[j] = omega_j[j] * functionDeriv(hidden[j]);
            
            indkj = k * NUM_HIDDEN + j;
            weightskj[indkj] += learningFactorkj * inputs[k] * psi_j[j];
         }    // for (int j = 0; j < NUM_HIDDEN; j++)
         
      }       // for (int k = 0; k < NUM_INPUTS; k++)
      
   }          // public void backward()
   
   
   /*
    * Randomizes and sets weights in given weightArray to 
    * random numbers between -MAX_WEIGHT / 2.0 and MAX_WEIGHT / 2.0
    * 
    * @param weightArray    array of weights to be randomized
    * @param numElements    number of elements in the weightArray
    */
   public void randomizeWeights(double[] weightArray, int numElements)
   {
      for (int index = 0; index < numElements; index++)
      {
         weightArray[index] = (MAX_WEIGHT * Math.random()) - 
                              (MAX_WEIGHT / 2.0);
      }
      
      return;
   }
   
   /*
    * This method randomizes weights if the program is training.
    * If the programming is running, this method sets weights to 
    * weights given by file  with the name weightsFile if running.
    */
   public void setWeights() throws IOException
   {
      String weightsFile = "weights.in";
      
      /*
       * If user types 1, set weights to random weights
       */
      if (shouldTrain == 1) 
      {
         randomizeWeights(weightskj, numElementskj);
         randomizeWeights(weightsji, numElementsji);
      }
      
      /*
       * If user types anything else, set weights to saved 
       * weights from file with name weightsFile
       */
      else                  
      {
         BufferedReader f1 = new BufferedReader(new FileReader(weightsFile));
         
         readWeights(f1, weightskj, numElementskj);
         readWeights(f1, weightsji, numElementsji);
      }
      
      return;
   }
   
   /*
    * This method reads in and sets weights to weights
    * given by the reader of the file containing weights.
    * 
    * @param fWeights     BufferedReader of the file containing saved weights
    * @param weightArray  array of weights to be set
    * @param numElements  number of elements in the weightArray
    */
   public void readWeights(BufferedReader fWeights, 
                           double[] weightArray, int numElements)
                           throws IOException
   {
      StringTokenizer st = new StringTokenizer(fWeights.readLine());
      
      for (int index = 0; index < numElements; index++)
      {
         weightArray[index] = Double.parseDouble(st.nextToken());
      }
      
      return;
   }
   
   /*
    * This method reads in and sets testInput and testOutput arrays
    * to the inputs and outputs for the test case that follows the
    * reader's current place in the file containing the test cases.
    * 
    * st3 - StringTokenizer for line from file containing test case inputs
    * st4 - StringTokenizer for line from file containing 
    *       test case expected outputs
    * 
    * @param fTest    BufferedReader of the file contianing test cases
    */
   public void setTestCases(BufferedReader fTest) throws IOException
   {
      StringTokenizer st3 = new StringTokenizer(fTest.readLine());
      StringTokenizer st4 = new StringTokenizer(fTest.readLine()); 
      
      
      for (int k = 0; k < NUM_INPUTS; k++)  // Read inputs into an array
      {
         testInput[k] = (double)Integer.parseInt(st3.nextToken(), 16);
      }
      
      
      for (int i = 0; i < NUM_OUTPUTS; i++) // Read test outputs into an array
      {
         testOutput[i] = Double.parseDouble(st4.nextToken());
      }
      
      return;
   }
   
   /*
    * This method sets the inputs in the current test case to the
    * program's inputs array in order that the program can be run
    * and the outputs can be found. This step is not necessarily
    * necessary, but it makes the whole running process easier to
    * conceptualize.
    */
   public void setInputs()
   {
      for (int k = 0; k < NUM_INPUTS; k++) // inputs = testInput when training
      {
         inputs[k] = testInput[k];
      }
      
      return;
   }
   
   /*
    * This method calculates the error over the outputs. Per
    * to the equation for error given by Dr. Nelson's handout,
    * findError() adds together all the errors over the outputs
    * to give the error for the current test case, the discrepancy
    * between the output given by the inputs and the current weights 
    * and the desired output.
    * 
    * omega[i] = desired output - calculated output
    * 
    * @return     the error for the current test case
    */
   public double findError()
   {
      double error = 0;
      
      for (int i = 0; i < NUM_OUTPUTS; i++)
      {
         double difference = omega_i[i];
         error += (1.0 / 2.0) * difference * difference;
      }
      
      return error;
   }
   
   /*
    * This method sets new weights after each iteration of the training loop
    * according to the steepest descent method.
    * 
    * @param weightsArray     array of weights to be set
    * @param errorDerivs      array of derivatives of error corresponding 
    *                         to the weights
    * @param learningFactor   learning factor for this calculation
    */
   public void setNewWeights(double[] weightsArray, double[] errorDerivs, 
                              double learningFactor)
   {
      for (int index = 0; index < weightsArray.length; index++)
      {
         weightsArray[index] += -learningFactor * errorDerivs[index];
      }
      
      return;
   }
   
   /*
    * This method prints results after program is done: 
    * 
    * If training, this method prints
    *    final weights
    *    total error
    *    reason for ending program
    *    output for the last case
    * 
    * If running, this method prints
    *    weights used
    *    outputs for the given inputs
    *    
    * The method also prints outputs to a file -- this is most useful 
    * for when the neural network is being run with inputs.
    * 
    * @param totalError   if training, total error of the program;
    *                     else, total error = INITIAL_ERROR, but totalError 
    *                           will not be printed in this case
    */
   public void printResults(double totalError) throws IOException
   {
      String outputFile = "outputs.out";
      String weightFile = "weights.out";
      
      PrintWriter outWeights = new PrintWriter(new BufferedWriter(new 
                                        FileWriter(weightFile)));
      
      outWeights.printf("k-j weights: \n");   //print k-j weights
      
      for (int kj = 0; kj < numElementskj; kj++)
      {
         outWeights.printf("%.10f ",weightskj[kj]);
      }
      
      outWeights.print("\n");
      
      
      
      outWeights.printf("j-i weights: ");     //print j-i weights  
      
      for (int ji = 0; ji < numElementsji; ji++)
      {
         outWeights.printf("%.10f ", weightsji[ji]);
      }
      
      outWeights.print("\n");
      
      outWeights.close();
      
      
      /*
       * Print reason for program stopping
       */
      if (shouldTrain == 1)                    
      {
         System.out.printf("TOTAL ERROR: %f\n\n",totalError);
      
         if (totalError < MIN_ERROR)
            System.out.printf("Program stopped because total error < MIN_ERROR\n");
         else
            System.out.printf("Program stopped because count > MAX_COUNT\n");
      } // if (shouldTrain == 1) 
      
      PrintWriter out = new PrintWriter(new BufferedWriter(new 
                                        FileWriter(outputFile)));
      
      /*
       * Print outputs to console and file
       */
      if (shouldTrain == 1)
      {
         System.out.printf("output: ");
         out.printf("output: ");
         
         for (int i = 0; i < NUM_OUTPUTS; i++)     
         {
            System.out.printf("%f\n",outputs[i]);
            out.printf("%f\n", outputs[i]);
         }
      }
      
      
      out.close();
      System.exit(0);
      
      return;
   } // public void printResults(double totalError) throws IOException
   
   
   /*
    * This method reads in inputs from the file with the name inputFile and 
    * runs the network with the given inputs, finding the outputs. The program
    * first reads the first line of inputFile, which contains the number sets
    * of inputs for which to find the outputs. Then, the method loops over the
    * sets of inputs and prints out the outputs for each set. Each subsequent line 
    * of the input file after the first represents a set of inputs.
    * 
    * This method is called when the program is running, not training.
    */
   public void runWithInputs() throws IOException
   {
      String inputFile = "OCR_inputset.in";
      
      setWeights();
      
      BufferedReader f3 = new BufferedReader(new FileReader(inputFile));
      
      int numInputs = Integer.parseInt(f3.readLine());
      
      for (int inCount = 0; inCount < numInputs; inCount++)
      {
         StringTokenizer st5 = new StringTokenizer(f3.readLine());
         
         for (int k = 0; k < NUM_INPUTS; k++)
         {
            inputs[k] = (double)Integer.parseInt(st5.nextToken(), 16);
         }
        
         
         
         outputs = findOutput(); // finds output of inputs
         
         /*
          * Prints outputs to the command window
          */
         System.out.printf("outputs: ");
         
         for (int i = 0; i < NUM_OUTPUTS; i++)
         {
            System.out.printf("%f ", outputs[i]);
         }
         
         System.out.print("\n");
      }
      
      return;
   } // public void runWithInputs() throws IOException
   
   /*
    * This method returns array of output node values from inputs and 
    * weights saved in the instance variable arrays. It performs the 
    * propagation rule and activation rule to find the hidden node values,
    * then does the same to find the output node values according to
    * Dr. Nelson's notes. The equations for the propagation and activation
    * rules are shown in the top class comment for NeuralNets.
    * 
    * Note: the weight indices (indkj, indji) are set differently
    * because the weight arrays are arranged according to this specification:
    * 
    *       kj weights:   w11 w12 ... w1n; w21 w22 ... w2n; w31...
    * 
    * where the first number is the input layer index and 
    *       the second number is the hidden layer index and
    *       n is the number of hidden layers
    * 
    *       ji weights:   w11 w12 ... w1m; w21 w22 ... w2m; w31...
    * 
    * where the first number is the hidden layer index and 
    *       the second number is the output layer index and
    *       m is the number of output layers
    *       
    * Therefore, to get to the desired weight in the weight array,
    * the index must be (k * NUM_HIDDEN + j) for kj weights.
    * 
    * @return       array of outputs
    */
   public double[] findOutput() 
   {
      double[] outputs = new double[NUM_OUTPUTS];
      
      int indkj = 0;                  // index of weightskj array
      
      /*
       * Finding the values of the hidden nodes
       */
      for (int j = 0; j < NUM_HIDDEN; j++)
      {
         double propagationj = 0.0;   // result of propagation rule for j-layer
         
         for (int k = 0; k < NUM_INPUTS; k++)
         {
            indkj = k * NUM_HIDDEN + j;
            propagationj += weightskj[indkj] * inputs[k];
         }
         
         hidden[j] = activationFunction(propagationj);
      }                               // for (int j = 0; j<NUM_HIDDEN; j++)
            
       
     
      
      int indji = 0;                  // index of weightsji array
      
      /*
       * Finding the values of the output nodes
       */
      for (int i = 0; i < NUM_OUTPUTS; i++)
      {
         double propagationi = 0.0;   // Result of propagation rule for i-layer
         
         for (int j = 0; j < NUM_HIDDEN; j++)           
         {
            indji = j * NUM_OUTPUTS + i;
            propagationi += weightsji[indji] * hidden[j];
         } 
               
         outputs[i] = activationFunction(propagationi);
      }                               // for (int i = 0; i < NUM_OUTPUTS; i++)
      
      return outputs;
   }                                  // public double[] findOutput() 

   
   /*
    * Returns the result of the activation function given the propagation 
    * value, in this case the sigmoid function.
    * 
    * @param propagation     propagation value
    * 
    * @return                activation value
    */
   public double activationFunction(double propagation)
   {
      return 1.0 / (1.0 + Math.exp(-propagation));
   }
   
   /*
    * This method computes the derivative of the activation function.
    * For the sigmoid function, f'(x) = f(x) * (1 - f(x)).
    * 
    * @param funcOutput     output of the activation function, f(x) --
    *                       oftentimes will be a value of a node, 
    *                       the values of which are often found using the
    *                       activation function
    */
   public double functionDeriv(double funcOutput)
   {
      return funcOutput * (1.0 - funcOutput);
   }
      
   /*
    * This method finds the derivatives of the errors using the equation given by 
    * Dr. Nelson's notes and sets values in the errorDerivskj and errorDerivsji 
    * arrays accordingly.
    * 
    * 
    * double iSummationkj - the result of the part of the derivative equation
    *                       for k-j weights that sums over the output index
    * double iSummationji - the result of the part of the derivative equation
    *                       for j-i weights that sums over the output index
    * 
    * Equations from notes rewritten using this program's variable names
    * (these correspond directly to the equations in the multiple output
    * notes): 
    * 
    * Derivative of error with respect to wkj
    *    = -input[k] * fprimej * iSummationkj
    * 
    * Derivation of error with respect to wji
    *    = -iSummationji * fprimei * hidden[j]
    * 
    */
   public void findDerivs()
   {
      int indkj = 0;   //weightskj index
      int indji = 0;   //weightsji index
      
      /*
       * The for loop below calculates the derivatives of the errors
       * with respect to the k-j weights.
       */
      for (int k = 0; k < NUM_INPUTS; k++)
      {
              
         for (int j = 0; j < NUM_HIDDEN; j++)
         {
            double iSummationkj = 0.0;
            
            double fprimej =  functionDeriv(hidden[j]);
            
            
            for (int i = 0; i < NUM_OUTPUTS; i++)
            {
               double fprimei = functionDeriv(outputs[i]);
               iSummationkj += (testOutput[i] - outputs[i]) * fprimei * 
                                 weightsji[j * NUM_OUTPUTS + i];
            }
            
            errorDerivskj[indkj] = -inputs[k] * fprimej * iSummationkj;
            
            indkj++;
         } // for (int j = 0; j < NUM_HIDDEN; j++)
         
      }    // for (int k = 0; k<NUM_INPUTS; k++)
     
      
      /*
       * The for loop below calculates the derivatives of the errors
       * with respect to the j-i weights.
       */
      double iSummationji = 0.0;
      
      for (int i = 0; i < NUM_OUTPUTS; i++)
      {
         iSummationji += testOutput[i] - outputs[i];
      }
      
      indji = 0;
      for (int j = 0; j < NUM_HIDDEN; j++)
      {
         
         for (int i = 0; i < NUM_OUTPUTS; i++)
         {
            double fprimei = functionDeriv(outputs[i]);
            
            errorDerivsji[indji] = -iSummationji * fprimei * hidden[j];
                     
            indji++;
         } // for (int j = 0; j < NUM_HIDDEN; j++)
         
      }    // for (int i = 0; i < NUM_OUTPUTS; i++)
      
      
      return;
   }       // public void findDerivs()
   
}          // public class NeuralNets