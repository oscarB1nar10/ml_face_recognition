package com.b1nar10.ml_face_recognition.data

import android.graphics.Bitmap
import android.util.Log
import com.b1nar10.ml_face_recognition.data.local_datasource.EmbeddingDao
import com.b1nar10.ml_face_recognition.data.local_datasource.EmbeddingEntity
import com.b1nar10.ml_face_recognition.data.local_datasource.PersonaDao
import com.b1nar10.ml_face_recognition.data.local_datasource.PersonaEntity
import com.b1nar10.ml_face_recognition.data.local_datasource.PersonaWithEmbeddingsDao
import com.b1nar10.ml_face_recognition.data.model.PersonModel
import com.b1nar10.ml_face_recognition.data.utility.Converters
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import javax.inject.Inject
import kotlin.math.pow

/**
 * Implementation of the FaceAnalyzerRepository.
 * This class analyzes input face images using a TensorFlow Lite model, and matches them
 * against known embeddings from the database.
 */
class FaceAnalyzerImpl @Inject constructor(
    private val interpreter: Interpreter,
    private val personaDao: PersonaDao, // Data access object for the Room database
    private val embeddingDao: EmbeddingDao,
    private val personaWithEmbeddingsDao: PersonaWithEmbeddingsDao
) : FaceAnalyzerRepository {
    private val TAG = "FaceAnalyzerImpl"
    private val tensorImage: TensorImage = TensorImage(DataType.FLOAT32)

    /**
     * Analyzes an input face image.
     * 1. Normalizes the image
     * 2. Runs the TFLite model to get an embedding
     * 3. Matches the embedding against known embeddings in the database
     * @param bitmapImage The input face image.
     * @return Name of the recognized person or "Unknown" if no match is found.
     */
    override suspend fun analyzeFaceImage(bitmapImage: Bitmap): PersonModel {
        // Normalize the image
        val tensorImage = normalizeImage(bitmapImage)
        val outputEmbeddingSize = Array(1) { FloatArray(FEATURE_VECTOR_SIZE) }
        interpreter.run(tensorImage.buffer, outputEmbeddingSize)

        // Match the embedding against the database
        val recognizedName = findClosestMatch(outputEmbeddingSize[0])

        return PersonModel(personName = recognizedName, bitMapImage = bitmapImage)
    }

    override suspend fun saveNewFace(personModel: PersonModel): Boolean {
        return try {
            val tensorImage = normalizeImage(personModel.bitMapImage)
            val outputEmbeddingSize = Array(1) { FloatArray(FEATURE_VECTOR_SIZE) }
            interpreter.run(tensorImage.buffer, outputEmbeddingSize)

            val ids1 = embeddingDao.getAllIds()
            println("ids1: $ids1")

            // User does not exist, save them
            val serializedEncoding = Converters.fromFloatArrayToJson(outputEmbeddingSize[0])
            val persona =
                PersonaEntity(id = personModel.id, name = personModel.personName)
            val personInsert = personaDao.insertPersona(persona)
            println("personInsert: $personInsert")

            val personaIds = personaDao.getPersonaIds()
            println("personaIds: $personaIds")

            val ids = embeddingDao.getAllIds()
            println("ids: $ids")
            val maxId = if (ids.isEmpty()) 0 else ids.max()
            val newEmbeddingId = maxId + 1
            val embeddingSaved = embeddingDao.insertEmbedding(
                EmbeddingEntity(
                    id = newEmbeddingId,
                    encoding = serializedEncoding,
                    personaId = personModel.id
                )
            )

            println("embeddingSaved: $embeddingSaved")

            // Return true to indicate success
            true
        } catch (e: Exception) {
            // Log the error and return false to indicate failure
            Log.e(TAG, "Error saving new face: ", e)
            false
        }
    }

    /**
     * Matches an embedding against known embeddings in the database.
     * @param embedding The embedding to match.
     * @return Name of the closest match or "Unknown".
     */
    private suspend fun findClosestMatch(embedding: FloatArray): String {
        try {
            val knownEmbeddings = embeddingDao.getEmbeddings()

            var minDistance = Float.MAX_VALUE
            var userId = ""

            // Calculate the distance between the input embedding and each known embedding
            for (knownEmbedding in knownEmbeddings) {
                val distance =
                    calculateDistance(
                        embedding,
                        Converters.fromJsonToFloatArray(knownEmbedding.encoding)
                    )
                if (distance < minDistance) {
                    minDistance = distance
                    userId = knownEmbedding.personaId
                }
            }

            // Only return the closest match if it's below a certain threshold
            val recognitionThreshold = 0.5f
            return if (minDistance < recognitionThreshold) {
                personaDao.getPersona(userId).name
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Calculates the Euclidean distance between two embeddings.
     * @param embedding1 The first embedding.
     * @param embedding2 The second embedding.
     * @return The Euclidean distance.
     */
    private fun calculateDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        return kotlin.math.sqrt(
            embedding1.zip(embedding2).sumOf { (a, b) -> (a - b).toDouble().pow(2.0) }.toFloat()
        )
    }

    /**
     * Normalizes an input image to be suitable for the model.
     * 1. Crops/pads the image to 160x160.
     * 2. Resizes the image to 160x160.
     * 3. Normalizes the pixel values.
     * @param bitmapImage The input image.
     * @return The normalized image.
     */
    private fun normalizeImage(bitmapImage: Bitmap): TensorImage {
        // Define the preprocessing operations
        val imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeWithCropOrPadOp(
                    IMAGE_TARGET_SIZE,
                    IMAGE_TARGET_SIZE
                )
            )  // Crop/pad the image to the target size
            .add(
                ResizeOp(
                    IMAGE_TARGET_SIZE,
                    IMAGE_TARGET_SIZE,
                    ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )  // Resize to the target size
            .add(NormalizeOp(0f, 255f))  // Normalize pixel values
            .build()

        // Apply the operations
        tensorImage.load(bitmapImage)
        return imageProcessor.process(tensorImage)
    }

    companion object {
        const val IMAGE_TARGET_SIZE = 224
        const val FEATURE_VECTOR_SIZE = 1280
    }
}