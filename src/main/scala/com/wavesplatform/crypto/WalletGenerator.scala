package com.wavesplatform.crypto

import java.io.File

import com.wavesplatform.settings.WalletSettings
import scorex.account.AddressScheme
import scorex.wallet.Wallet

object WalletGenerator extends App {
  AddressScheme.current = new AddressScheme {
    override val chainId: Byte = 'G'
  }
  val f = new File("/tmp/wallet.dat")
  if (f.exists()) f.delete()
  val w = Wallet(WalletSettings(Some(f), "qwertyuio"))
  w.generateNewAccounts(5).foreach(p => println(p.address))
}
