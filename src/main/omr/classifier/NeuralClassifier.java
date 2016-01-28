//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 N e u r a l C l a s s i f i e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import static omr.classifier.AbstractClassifier.shapeCount;

import omr.math.NeuralNetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import javax.xml.bind.JAXBException;
import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

/**
 * Class {@code NeuralClassifier} encapsulates a neural network customized for glyph
 * recognition.
 * <p>
 * It wraps the generic {@link NeuralNetwork} with application information, for training, storing,
 * loading and using the neural network.
 * <p>
 * The application neural network data is loaded as follows: <ol>
 * <li>It first tries to find a file named 'eval/neural-network.xml' in the application user area.
 * If any, this file contains a custom definition of the network, typically after a user
 * training.</li>
 * <li>If not found, it falls back reading the default definition from the application resource,
 * reading the 'res/neural-network.xml' file in the application program area.</ol></p>
 * <p>
 * After a user training of the neural network, the data is stored as the custom definition in the
 * user local file 'eval/neural-network.xml', which will be picked up first when the application is
 * run again.</p>
 *
 * @author Hervé Bitteur
 */
public class NeuralClassifier
        extends AbstractClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(NeuralClassifier.class);

    /** The singleton. */
    private static volatile NeuralClassifier INSTANCE;

    /** Neural network file name. */
    private static final String FILE_NAME = "neural-network.xml";

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The underlying neural network. */
    private NeuralNetwork engine;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor, to create a glyph neural network.
     */
    private NeuralClassifier ()
    {
        // Unmarshal from user or default data, if compatible
        engine = (NeuralNetwork) unmarshal();

        if (engine == null) {
            // Get a brand new one (not trained)
            logger.info("Creating a brand new {}", getName());
            engine = createNetwork();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    /**
     * Report a name for this network.
     *
     * @return a simple name
     */
    @Override
    public final String getName ()
    {
        return "Neural Network";
    }

    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the neural network to the standard output.
     */
    @Override
    public void dump ()
    {
        engine.dump();
    }

    //--------------//
    // getAmplitude //
    //--------------//
    /**
     * Selector for the amplitude value (used in initial random values).
     *
     * @return the amplitude value
     */
    public double getAmplitude ()
    {
        return constants.amplitude.getValue();
    }

    //
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of NeuralClassifier in the application.
     *
     * @return the instance
     */
    public static NeuralClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (NeuralClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NeuralClassifier();
                    logger.info("Using {} as shape classifier", INSTANCE.getName());
                }
            }
        }

        return INSTANCE;
    }

    //-----------------//
    // getLearningRate //
    //-----------------//
    /**
     * Selector of the current value for network learning rate.
     *
     * @return the current learning rate
     */
    public double getLearningRate ()
    {
        return constants.learningRate.getValue();
    }

    //---------------//
    // getListEpochs //
    //---------------//
    /**
     * Selector on the maximum number of training iterations.
     *
     * @return the upper limit on iteration counter
     */
    public int getListEpochs ()
    {
        return constants.listEpochs.getValue();
    }

    //-------------//
    // getMaxError //
    //-------------//
    /**
     * Report the error threshold to potentially stop the training
     * process.
     *
     * @return the threshold currently in use
     */
    public double getMaxError ()
    {
        return constants.maxError.getValue();
    }

    //-------------//
    // getMomentum //
    //-------------//
    /**
     * Report the momentum training value currently in use.
     *
     * @return the momentum in use
     */
    public double getMomentum ()
    {
        return constants.momentum.getValue();
    }

    //-----------------------//
    // getNaturalEvaluations //
    //-----------------------//
    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph,
                                               int interline)
    {
        double[] ins = ShapeDescription.features(glyph, interline);
        double[] outs = new double[shapeCount];
        Evaluation[] evals = new Evaluation[shapeCount];
        Shape[] values = Shape.values();

        engine.run(ins, null, outs);
        normalize(outs);

        for (int s = 0; s < shapeCount; s++) {
            Shape shape = values[s];
            // Use a grade in 0 .. 1 range
            evals[s] = new Evaluation(shape, outs[s]);
        }

        return evals;
    }

    //------------//
    // getNetwork //
    //------------//
    /**
     * Selector to the encapsulated Neural Network.
     *
     * @return the neural network
     */
    public NeuralNetwork getNetwork ()
    {
        return engine;
    }

    //--------------//
    // setAmplitude //
    //--------------//
    /**
     * Set the amplitude value for initial random values (UNUSED).
     *
     * @param amplitude
     */
    public void setAmplitude (double amplitude)
    {
        constants.amplitude.setValue(amplitude);
    }

    //-----------------//
    // setLearningRate //
    //-----------------//
    /**
     * Dynamically modify the learning rate of the neural network for
     * its training task.
     *
     * @param learningRate new learning rate to use
     */
    public void setLearningRate (double learningRate)
    {
        constants.learningRate.setValue(learningRate);
        engine.setLearningRate(learningRate);
    }

    //---------------//
    // setListEpochs //
    //---------------//
    /**
     * Modify the upper limit on the number of epochs (training
     * iterations) for the training process.
     *
     * @param listEpochs new value for iteration limit
     */
    public void setListEpochs (int listEpochs)
    {
        constants.listEpochs.setValue(listEpochs);
        engine.setEpochs(listEpochs);
    }

    //-------------//
    // setMaxError //
    //-------------//
    /**
     * Modify the error threshold to potentially stop the training
     * process.
     *
     * @param maxError the new threshold value to use
     */
    public void setMaxError (double maxError)
    {
        constants.maxError.setValue(maxError);
        engine.setMaxError(maxError);
    }

    //-------------//
    // setMomentum //
    //-------------//
    /**
     * Modify the value for momentum used from learning epoch to the
     * other.
     *
     * @param momentum the new momentum value to be used
     */
    public void setMomentum (double momentum)
    {
        constants.momentum.setValue(momentum);
        engine.setMomentum(momentum);
    }

    //------//
    // stop //
    //------//
    /**
     * Forward the "Stop" order to the network being trained.
     */
    @Override
    public void stop ()
    {
        engine.stop();
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the network using the provided collection of glyphs.
     *
     * @param samples the provided collection of shapes samples
     * @param monitor the monitoring entity if any
     * @param mode    the starting mode of the trainer (scratch or incremental)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void train (Collection<Sample> samples,
                       Monitor monitor,
                       StartingMode mode)
    {
        if (samples.isEmpty()) {
            logger.warn("No sample to retrain Neural Network classifier");

            return;
        }

        int quorum = constants.quorum.getValue();

        // Determine cardinality for each shape
        EnumMap<Shape, List<Sample>> shapesMap = new EnumMap<Shape, List<Sample>>(
                Shape.class);

        for (Sample sample : samples) {
            Shape shape = sample.shape;
            List<Sample> list = shapesMap.get(shape);

            if (list == null) {
                list = new ArrayList<Sample>();
                shapesMap.put(shape, list);
            }

            list.add(sample);
        }

        List<Sample> newSamples = new ArrayList<Sample>();

        for (List<Sample> list : shapesMap.values()) {
            int card = 0;
            boolean first = true;

            if (!list.isEmpty()) {
                while (card < quorum) {
                    for (int i = 0; i < list.size(); i++) {
                        newSamples.add(list.get(i));
                        card++;

                        if (!first && (card >= quorum)) {
                            break;
                        }
                    }

                    first = false;
                }
            }
        }

        // Shuffle the final collection of glyphs
        Collections.shuffle(newSamples);

        // Build the collection of patterns from the glyph data
        double[][] inputs = new double[newSamples.size()][];
        double[][] desiredOutputs = new double[newSamples.size()][];

        int ig = 0;

        for (Sample sample : newSamples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());
            inputs[ig] = ins;

            double[] des = new double[shapeCount];
            Arrays.fill(des, 0);

            des[sample.getShape().getPhysicalShape().ordinal()] = 1;
            desiredOutputs[ig] = des;

            ig++;
        }

        // Starting options
        if (mode == StartingMode.SCRATCH) {
            engine = createNetwork();
        }

        // Train on the patterns
        engine.train(inputs, desiredOutputs, monitor);
    }

    //-------------//
    // getFileName //
    //-------------//
    @Override
    protected String getFileName ()
    {
        return FILE_NAME;
    }

    //--------------//
    // isCompatible //
    //--------------//
    @Override
    protected final boolean isCompatible (Object obj)
    {
        if (obj instanceof NeuralNetwork) {
            NeuralNetwork anEngine = (NeuralNetwork) obj;

            if (!Arrays.equals(anEngine.getInputLabels(), ShapeDescription.getParameterLabels())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Engine inputs: {}", Arrays.toString(anEngine.getInputLabels()));
                    logger.debug(
                            "Shape  inputs: {}",
                            Arrays.toString(ShapeDescription.getParameterLabels()));
                }

                return false;
            }

            if (!Arrays.equals(anEngine.getOutputLabels(), ShapeSet.getPhysicalShapeNames())) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Engine  outputs: {}",
                            Arrays.toString(anEngine.getOutputLabels()));
                    logger.debug(
                            "Physical shapes: {}",
                            Arrays.toString(ShapeSet.getPhysicalShapeNames()));
                }

                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    //---------//
    // marshal //
    //---------//
    @Override
    protected void marshal (OutputStream os)
            throws FileNotFoundException, IOException, JAXBException
    {
        engine.marshal(os);
    }

    //-----------//
    // unmarshal //
    //-----------//
    @Override
    protected NeuralNetwork unmarshal (InputStream is)
            throws JAXBException, IOException
    {
        return NeuralNetwork.unmarshal(is);
    }

    //---------------//
    // createNetwork //
    //---------------//
    private NeuralNetwork createNetwork ()
    {
        // Note : We allocate a hidden layer with as many cells as the output
        // layer
        NeuralNetwork nn = new NeuralNetwork(
                ShapeDescription.length(),
                shapeCount,
                shapeCount,
                getAmplitude(),
                ShapeDescription.getParameterLabels(), // Input labels
                ShapeSet.getPhysicalShapeNames(), // Output labels
                getLearningRate(),
                getMomentum(),
                getMaxError(),
                getListEpochs());

        return nn;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Adjust all values, so that their sum equals 1
     *
     * @param vals the probability values
     */
    private void normalize (double[] vals)
    {
        double sum = 0;

        for (double val : vals) {
            sum += val;
        }

        for (int i = 0; i < vals.length; i++) {
            vals[i] /= sum;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio amplitude = new Constant.Ratio(
                0.5,
                "Initial weight amplitude");

        private final Constant.Ratio learningRate = new Constant.Ratio(0.2, "Learning Rate");

        private final Constant.Integer listEpochs = new Constant.Integer(
                "Epochs",
                500,
                "Number of epochs for training on list of glyphs");

        private final Constant.Integer quorum = new Constant.Integer(
                "Glyphs",
                10,
                "Minimum number of glyphs for each shape");

        private final Evaluation.Grade maxError = new Evaluation.Grade(
                1E-3,
                "Threshold to stop training");

        private final Constant.Ratio momentum = new Constant.Ratio(0.2, "Training momentum");
    }
}