package org.opennars.control;

import org.opennars.entity.BudgetValue;
import org.opennars.entity.Sentence;
import org.opennars.entity.Stamp;
import org.opennars.entity.Task;
import org.opennars.main.Parameters;
import org.opennars.storage.Memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventInferenceDeriver {
    public double timeToTruthDecay = 0.001; // config

    public int bagMaxSize = 200; // config

    public List<StatementWithAttentionValue> bag = new ArrayList<>();

    /**
     * tries to select random premises and inferes conclusions from them
     * @param time
     * @param mem
     * @param narParameters
     */
    public void tryInfer(long time, Memory mem, Parameters narParameters) {
        if (bag.size() <= 1) {
            return;
        }

        int idxA, idxB;
        for(;;) {
            idxA = mem.randomNumber.nextInt(bag.size());
            idxB = mem.randomNumber.nextInt(bag.size());
            if (idxA != idxB) {
                break;
            }
        }

        StatementWithAttentionValue a = bag.get(idxA);
        StatementWithAttentionValue b = bag.get(idxB);

        Sentence premiseASentence = a.sentence;
        Sentence premiseBSentence = b.sentence;

        if (premiseASentence.term.equals(premiseBSentence.term)) {
            return; // no need to reason about the same event happening at the same time
        }

        // must not overlap
        if (Stamp.baseOverlap(premiseASentence.stamp, premiseBSentence.stamp)) {
            return;
        }

        // stuff it all into the deriver
        List<Sentence> conclusionSentences = new ArrayList<>();
        mem.trieDeriver.derive(premiseASentence, premiseBSentence, conclusionSentences, time, narParameters);


        // add results to memory
        {
            for(Sentence iConclusionSentence : conclusionSentences) {
                BudgetValue budget = new BudgetValue(0.9f, 0.5f, 0.5f, narParameters);

                // we need to eternalize the sentence
                // TODO< do it the proper way with calculation of TV >
                Stamp eternalizedStamp = iConclusionSentence.stamp.clone();
                eternalizedStamp.setEternal();
                Sentence eternalizedSentence = new Sentence(iConclusionSentence.term, iConclusionSentence.punctuation, iConclusionSentence.truth, eternalizedStamp);

                Task createdTask = new Task(
                    eternalizedSentence,
                    budget,
                    Task.EnumType.DERIVED
                );

                mem.addNewTask(createdTask, "Derived");
            }
        }

        // put results back into bag and sort
        for(Sentence iConclusion : conclusionSentences) {
            // TODO< add it only if it doesn't exist already based on statement and stamp >

            bag.add(new StatementWithAttentionValue(iConclusion));
        }

        // sort
        Collections.sort(bag, (s1, s2) -> s1.calcPriority(time) < s2.calcPriority(time) ? 1 : -1);

        // limit memory
        while(bag.size() > bagMaxSize) {
            bag.remove(bagMaxSize);
        }
    }


    public class StatementWithAttentionValue {
        public Sentence sentence;

        public StatementWithAttentionValue(Sentence sentence) {
            this.sentence = sentence;
        }

        public double calcPriority(long currentTime) {
            long ageOfSentence = currentTime - sentence.stamp.getCreationTime();

            double weight = Math.exp(-(double)ageOfSentence * timeToTruthDecay);

            return weight*sentence.truth.getConfidence() + (1.0-weight)*(1.0/ageOfSentence);
        }
    }
}