Optical Character Recognition README
@author Helen Y. Wu
@version 04/14/15

Overview:

This was written as a project for my Neural Networks course. In this project, I used the backpropagation algorithm to train networks and implement Optical Character Recognition. 

BitmapDump.java serves to process the photos of the images before they are ready to be used as part of the training set or ready to be the input for the already-trained neural network. 

NeuralNets.java implements the backpropagation training algorithm given an input set and its expected output, giving a text file with weights. After trained, NeuralNets.java can run the neural network on an unknown set and return the 6-digit binary number of the character that the input most resembles.

More detailed overview and documentation:

This program is able to train neural networks
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