package net.kdt.pojavlaunch.utils

import android.content.res.AssetManager
import android.util.ArrayMap
import android.util.Base64
import java.io.IOException
import java.io.InputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

class SignatureCheckUtil(private val mPublicKey: PublicKey?) {
    /**
     * Verifies the signature of an input stream against the cert.pem certificate from app assets
     * @param inputStream the original file stream
     * @param signatureBytes the bytes of the encrypted signature
     * @return whether the file signature check passed or not
     * @throws IOException if there was an error while reading the file
     */
    @Throws(IOException::class)
    fun verify(inputStream: InputStream, signatureBytes: ByteArray?): Boolean {
        val ingestionBuffer = ByteArray(65535)
        try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(mPublicKey)
            var i = 0
            while (i != -1) {
                signature.update(ingestionBuffer, 0, i)
                i = inputStream.read(ingestionBuffer)
            }
            return signature.verify(signatureBytes)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: SignatureException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        /**
         * Decode a bundle of signatures. A bundle of signatures has the following format:
         * fileName1:base64-rsa4096-signature
         * fileName2:base64-rsa4096-signature
         * Invalid signatures aren't included in the resulting Map.
         * @param bundle the original string of the bundle
         * @return each decoded signature mapped to each file name
         */
        @JvmStatic
        fun decodeSignatureBundle(bundle: String): MutableMap<String?, ByteArray?> {
            val signatureLines =
                bundle.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val signatures = ArrayMap<String?, ByteArray?>(signatureLines.size)
            for (signatureLine in signatureLines) {
                val splitSignLine =
                    signatureLine.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitSignLine.size != 2) continue
                try {
                    val signatureBytes: ByteArray? = decodeRsa4096FromBase64(splitSignLine[1])
                    if (signatureBytes == null) continue
                    signatures.put(splitSignLine[0], signatureBytes)
                } catch (ignored: IllegalArgumentException) {
                }
            }
            return signatures
        }

        /**
         * Decode an RSA4096-encrypted signature from a Base64 string
         * @param base64 the original base64 data
         * @return the decoded bytes, or null if the data length isn't correct
         */
        fun decodeRsa4096FromBase64(base64: String?): ByteArray? {
            val rsaBytes = Base64.decode(base64, Base64.DEFAULT)
            if (rsaBytes.size != 512) return null
            return rsaBytes
        }

        /**
         * Reads in the cert.pem certificate from application assets and creates a SignatureCheckUtil
         * to verify data against this certificate
         * @param assetManager the AssetManager used to read the cert.pem file
         * @return the SignatureCheckUtil instance
         * @throws IOException if reading fails
         */
        @JvmStatic
        @Throws(IOException::class)
        fun create(assetManager: AssetManager): SignatureCheckUtil {
            try {
                assetManager.open("cert.pem").use { certificateStream ->
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val certificate = certificateFactory.generateCertificate(certificateStream)
                    return SignatureCheckUtil(certificate.getPublicKey())
                }
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            }
        }
    }
}
