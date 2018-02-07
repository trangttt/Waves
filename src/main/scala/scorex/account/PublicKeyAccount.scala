package scorex.account

import java.security.PublicKey

import ru.CryptoPro.JCP.Key.GostPublicKey
import scorex.crypto.encode.Base58
import scorex.transaction.TransactionParser
import scorex.transaction.ValidationError.InvalidAddress

trait PublicKeyAccount {
  def publicKey: PublicKey

  override def equals(b: Any): Boolean = b match {
    case a: PublicKeyAccount => publicKey.equals(a.publicKey)
    case _ => false
  }

  override def hashCode(): Int = publicKey.hashCode()

  override lazy val toString: String = this.toAddress.address
}

object PublicKeyAccount {

  private case class PublicKeyAccountImpl(publicKey: PublicKey) extends PublicKeyAccount

  def apply(publicKey: Array[Byte]): PublicKeyAccount = {PublicKeyAccountImpl(new GostPublicKey(publicKey))}

  implicit def toAddress(publicKeyAccount: PublicKeyAccount): Address = Address.fromPublicKey(publicKeyAccount.publicKey.getEncoded)

  implicit class PublicKeyAccountExt(pk: PublicKeyAccount) {
    def toAddress: Address = PublicKeyAccount.toAddress(pk)
  }

  def fromBase58String(s: String): Either[InvalidAddress, PublicKeyAccount] =
    (for {
      _ <- Either.cond(s.length <= TransactionParser.KeyStringLength, (), "Bad public key string length")
      bytes <- Base58.decode(s).toEither.left.map(ex => s"Unable to decode base58: ${ex.getMessage}")
    } yield PublicKeyAccount(bytes)).left.map(err => InvalidAddress(s"Invalid sender: $err"))
}
