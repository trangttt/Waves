package com.wavesplatform.lang.benchmark

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import com.wavesplatform.lang.benchmark.GlobalFunctionsBenchmark._
import com.wavesplatform.lang.traits.{DataType, Environment, Transaction}
import com.wavesplatform.lang.{EnvironmentFunctions, Global}
import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}
import scodec.bits.ByteVector
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, PrivateKey, PublicKey, Signature}

class GlobalFunctionsBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def sha256_test(): Array[Byte] = hashTest(Global.sha256)

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def keccack256_test(): Array[Byte] = hashTest(Global.keccack256)

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def blake2b256_test(): Array[Byte] = hashTest(Global.blake2b256)

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def secureHash_test(): Array[Byte] = hashTest(Global.secureHash)

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def curve25519_generateKeypair_test(): (PrivateKey, PublicKey) = curve25519.generateKeypair

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def curve25519_sign_test(): Array[Byte] = {
    val (privateKey, _) = curve25519.generateKeypair
    curve25519.sign(privateKey, randomBytes(DataBytesLength))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def curve25519_full_test(): Boolean = {
    val (privateKey, publicKey) = curve25519.generateKeypair
    val message                 = randomBytes(DataBytesLength)
    val signature               = curve25519.sign(privateKey, message)
    Curve25519.verify(Signature @@ signature, message, publicKey)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def addressFromPublicKey_test(): ByteVector = randomAddress

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def addressFromString_full_test(): Either[String, Option[ByteVector]] = environmentFunctions.addressFromString(Base58.encode(randomAddress.toArray))

}

object GlobalFunctionsBenchmark {

  val NetworkByte: Byte = 'P'
  val DataBytesLength   = 512
  val SeedBytesLength   = 128
  val PublicKeyBytes    = 32 // PublicKeyAccount.KeyLength

  private val defaultEnvironment: Environment = new Environment {
    override def height: Int                                                                   = 1
    override def networkByte: Byte                                                             = NetworkByte
    override def transaction: Transaction                                                      = ???
    override def transactionById(id: Array[Byte]): Option[Transaction]                         = ???
    override def data(addressBytes: Array[Byte], key: String, dataType: DataType): Option[Any] = ???
    override def resolveAddress(addressOrAlias: Array[Byte]): Either[String, Array[Byte]]      = ???
  }

  val environmentFunctions = new EnvironmentFunctions(defaultEnvironment)

  def randomBytes(length: Int): Array[Byte] = {
    val bytes = Array.fill[Byte](DataBytesLength)(0)
    ThreadLocalRandom.current().nextBytes(bytes)
    bytes
  }

  def randomAddress: ByteVector = environmentFunctions.addressFromPublicKey(ByteVector(randomBytes(PublicKeyBytes)))

  def hashTest(f: Array[Byte] => Array[Byte]): Array[Byte] = f(randomBytes(DataBytesLength))

  object curve25519 {
    def generateKeypair: (PrivateKey, PublicKey)                        = Curve25519.createKeyPair(randomBytes(SeedBytesLength))
    def sign(privateKey: PrivateKey, message: Array[Byte]): Array[Byte] = Curve25519.sign(privateKey, message)
  }
}
