/*
 * Copyright (c) 2011 James Smith <james@loopj.com>
 * Copyright (c) 2015 Fran Montiel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Based on the code from this stackoverflow answer http://stackoverflow.com/a/25462286/980387 by janoliver
 * Modifications in the structure of the class and addition of serialization of httpOnly attribute
 */

/*
 * Based on this gist by franmontiel:
 *
 * https://gist.github.com/franmontiel/ed12a2295566b7076161
 *
 * Changes: None so far.
 */

// STOPSHIP: Credit properly

package me.shreyasr.chatse.network.cookie


import android.util.Log

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.Field
import java.net.HttpCookie
import kotlin.experimental.and

class SerializableHttpCookie : Serializable {

    lateinit var cookie: HttpCookie

    // Workaround httpOnly: The httpOnly attribute is not accessible so when we
    // serialize and deserialize the cookie it not preserve the same value. We
    // need to access it using reflection
    private var fieldHttpOnly: Field? = null

    fun encode(cookie: HttpCookie): String? {
        this.cookie = cookie

        val os = ByteArrayOutputStream()
        try {
            val outputStream = ObjectOutputStream(os)
            outputStream.writeObject(this)
        } catch (e: IOException) {
            Log.d(TAG, "IOException in encodeCookie", e)
            return null
        }

        return byteArrayToHexString(os.toByteArray())
    }

    fun decode(encodedCookie: String): HttpCookie? {
        val bytes = hexStringToByteArray(encodedCookie)
        val byteArrayInputStream = ByteArrayInputStream(
                bytes)
        var cookie: HttpCookie? = null
        try {
            val objectInputStream = ObjectInputStream(
                    byteArrayInputStream)
            cookie = (objectInputStream.readObject() as SerializableHttpCookie).cookie
        } catch (e: IOException) {
            Log.d(TAG, "IOException in decodeCookie", e)
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "ClassNotFoundException in decodeCookie", e)
        }

        return cookie
    }

    // Workaround httpOnly (getter)
    // Workaround httpOnly (setter)
    private // NoSuchFieldException || IllegalAccessException ||
            // IllegalArgumentException
            // NoSuchFieldException || IllegalAccessException ||
            // IllegalArgumentException
    var httpOnly: Boolean
        get() {
            try {
                initFieldHttpOnly()
                return fieldHttpOnly!!.get(cookie) as Boolean
            } catch (e: Exception) {
                Log.w(TAG, e)
            }

            return false
        }
        set(httpOnly) {
            try {
                initFieldHttpOnly()
                fieldHttpOnly!!.set(cookie, httpOnly)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }

        }

    @Throws(NoSuchFieldException::class)
    private fun initFieldHttpOnly() {
        fieldHttpOnly = cookie.javaClass.getDeclaredField("httpOnly")
        fieldHttpOnly!!.isAccessible = true
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeObject(cookie.name)
        out.writeObject(cookie.value)
        out.writeObject(cookie.comment)
        out.writeObject(cookie.commentURL)
        out.writeObject(cookie.domain)
        out.writeLong(cookie.maxAge)
        out.writeObject(cookie.path)
        out.writeObject(cookie.portlist)
        out.writeInt(cookie.version)
        out.writeBoolean(cookie.secure)
        out.writeBoolean(cookie.discard)
        out.writeBoolean(httpOnly)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        val name = `in`.readObject() as String
        val value = `in`.readObject() as String
        cookie = HttpCookie(name, value)
        if(cookie.comment != null) {
            cookie.comment = `in`.readObject() as String
            cookie.commentURL = `in`.readObject() as String
            cookie.domain = `in`.readObject() as String
            cookie.maxAge = `in`.readLong()
            cookie.path = `in`.readObject() as String
            cookie.portlist = `in`.readObject() as String
            cookie.version = `in`.readInt()
            cookie.secure = `in`.readBoolean()
            cookie.discard = `in`.readBoolean()
            httpOnly = `in`.readBoolean()
        }
    }

    /**
     * Using some super basic byte array &lt;-&gt; hex conversions so we don't
     * have to rely on any large Base64 libraries. Can be overridden if you
     * like!

     * @param bytes byte array to be converted
     * *
     * @return string containing hex values
     */
    private fun byteArrayToHexString(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (element in bytes) {
            val v = element and 0xff.toByte()
            if (v < 16) {
                sb.append('0')
            }
            sb.append(Integer.toHexString(v.toInt()))
        }
        return sb.toString()
    }

    /**
     * Converts hex values from strings to byte array

     * @param hexString string of hex-encoded values
     * *
     * @return decoded byte array
     */
    private fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character
                    .digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    companion object {
        private val TAG = SerializableHttpCookie::class.java.simpleName

        private const val serialVersionUID = 6374381323722046732L
    }
}