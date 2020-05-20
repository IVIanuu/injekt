/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tschuchort.compiletesting

import java.io.IOException
import java.io.OutputStream


/** An output stream that does nothing, like /dev/null */
internal object NullStream : OutputStream() {
    override fun write(b: Int) {
        //NoOp
    }

    override fun close() {
        //NoOp
    }

    override fun flush() {
        //NoOp
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        //NoOp
    }

    override fun write(b: ByteArray) {
        //NoOp
    }
}

/** A combined stream that writes to all the output streams in [streams]. */
@Suppress("MemberVisibilityCanBePrivate")
internal class TeeOutputStream(val streams: Collection<OutputStream>) : OutputStream() {

    constructor(vararg streams: OutputStream) : this(streams.toList())

    @Synchronized
    @Throws(IOException::class)
    override fun write(b: Int) {
        for (stream in streams)
            stream.write(b)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        for (stream in streams)
            stream.write(b)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        for (stream in streams)
            stream.write(b, off, len)
    }

    @Throws(IOException::class)
    override fun flush() {
        for (stream in streams)
            stream.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        closeImpl(streams)
    }

    @Throws(IOException::class)
    private fun closeImpl(streamsToClose: Collection<OutputStream>) {
        try {
            streamsToClose.firstOrNull()?.close()
        } finally {
            if (streamsToClose.size > 1)
                closeImpl(streamsToClose.drop(1))
        }
    }
}