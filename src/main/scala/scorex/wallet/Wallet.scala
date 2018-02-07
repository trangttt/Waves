package scorex.wallet

import java.io.{ByteArrayInputStream, File, FileInputStream, FileOutputStream}
import java.security.{KeyPair, KeyPairGenerator, KeyStore, PrivateKey}
import java.security.cert._

import com.wavesplatform.settings.WalletSettings
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCPRequest.GostCertificateRequest
import scorex.account.{Address, PrivateKeyAccount, PublicKeyAccount}
import scorex.transaction.ValidationError
import scorex.utils.ScorexLogging
import sun.security.jca.JCAUtil

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

trait Wallet {

  def nonEmpty: Boolean

  def privateKeyAccounts: List[PrivateKeyAccount]

  def generateNewAccounts(howMany: Int): Seq[PublicKeyAccount]

  def generateNewAccount(): Option[PublicKeyAccount]

  def privateKeyAccount(account: Address): Either[ValidationError, PrivateKeyAccount]

}

object Wallet extends ScorexLogging {

  implicit class WalletExtension(w: Wallet) {
    def findWallet(addressString: String): Either[ValidationError, PrivateKeyAccount] = for {
      acc <- Address.fromString(addressString)
      privKeyAcc <- w.privateKeyAccount(acc)
    } yield privKeyAcc
  }

  lazy val kg: KeyPairGenerator = {
    val kg = KeyPairGenerator.getInstance(JCP.GOST_EL_2012_256_NAME, JCP.PROVIDER_NAME)
    kg.initialize(512, JCAUtil.getSecureRandom)
    kg
  }

  def generateNewAccount(): PrivateKeyAccount = {
    val pair = kg.generateKeyPair
    PrivateKeyAccount(pair.getPrivate, pair.getPublic)
  }

  def apply(settings: WalletSettings): Wallet = new WalletImpl(settings.file, settings.password)

  private class WalletImpl(file: Option[File], password: String)
    extends ScorexLogging with Wallet {

    private val keyStore = KeyStore.getInstance(JCP.HD_STORE_NAME, JCP.PROVIDER_NAME)
    file match {
      case Some(f) if f.exists() =>
        keyStore.load(new FileInputStream(f), password.toCharArray)
      case Some(f) if !f.exists() =>
        f.getParentFile.mkdirs()
        f.createNewFile()
        keyStore.load(null, null)
      case _ => keyStore.load(null, null)
    }

    override def privateKeyAccounts: List[PrivateKeyAccount] = {
      (for {
        // todo filter by waves prefix
        alias <- keyStore.aliases().asScala.toSeq
      } yield {
        val key = keyStore.getKey(alias, password.toCharArray)
        val cert = keyStore.getCertificate(alias)
        PrivateKeyAccount(key.asInstanceOf[PrivateKey], cert.getPublicKey)
      }).toList
    }

    def nonEmpty: Boolean = keyStore.aliases().hasMoreElements

    def generateNewAccounts(howMany: Int): Seq[PublicKeyAccount] =
      (1 to howMany).flatMap(_ => generateNewAccount())

    private def genSelfCert(pair: KeyPair, dname: String): Certificate = {
      val gr = new GostCertificateRequest()
      val enc = gr.getEncodedSelfCert(pair, dname)
      val cf = CertificateFactory.getInstance(JCP.CERTIFICATE_FACTORY_NAME)
      cf.generateCertificate(new ByteArrayInputStream(enc))
    }

    def generateNewAccount(): Option[PublicKeyAccount] = synchronized {
      val pair = kg.generateKeyPair
      val pka = PublicKeyAccount(pair.getPublic.getEncoded)
      keyStore.setKeyEntry(pka.address, pair.getPrivate, password.toCharArray, Array(genSelfCert(pair, "CN=Waves_2012_256, O=Waves, C=RU")))
      // todo do not store it like this?
      file.foreach(f => keyStore.store(new FileOutputStream(f), password.toCharArray))
      Some(pka)
    }

    def privateKeyAccount(account: Address): Either[ValidationError, PrivateKeyAccount] = {
      Try(keyStore.getCertificate(account.address) -> keyStore.getKey(account.address, password.toCharArray)) match {
        // todo construct it
        case Success((c, k)) => Right(PrivateKeyAccount(k.asInstanceOf[PrivateKey], c.getPublicKey))
        case Failure(f) => Left(ValidationError.MissingSenderPrivateKey)
      }
    }
  }
}
