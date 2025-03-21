package ru.samolet.indoorinspection.data

import org.tensorflow.lite.task.vision.classifier.Classifications

class EnsembleHelper {

        fun convertClassificationsToList(classifications: List<Classifications>?, areLogits: Boolean = false): HashMap<String, Float> {
            if (classifications != null && classifications.get(0).categories.size > 0) {
                val pan = HashMap<String, Float>()
                var normalizedFloats = FloatArray(classifications.get(0).categories.size)
                for (i in classifications.get(0).categories.indices) {
                    pan[classifications.get(0).categories.get(i).label] =
                        classifications.get(0).categories.get(i).score
                    normalizedFloats[i] = classifications.get(0).categories.get(i).score
                }
                if (areLogits) {
                    normalizedFloats = SoftmaxLayer().softmax(normalizedFloats)
                    for (i in classifications.get(0).categories.indices) {
                        pan[classifications.get(0).categories.get(i).label] = normalizedFloats[i]
                    }
                }
                return pan
            }

            return hashMapOf()
        }
}
