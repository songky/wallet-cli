package org.tron.walletcli;

import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.Node;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.keystore.StringUtils;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.walletserver.WalletClient;

public class TestClient {

  private static final Logger logger = LoggerFactory.getLogger("TestClient");
  private Client client = new Client();

  private char[] inputPassword2Twice() throws IOException {
    char[] password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = Utils.inputPassword(true);
      System.out.println("Please input password again.");
      char[] password1 = Utils.inputPassword(true);
      boolean flag = Arrays.equals(password0, password1);
      StringUtils.clear(password1);
      if (flag) {
        break;
      }
      System.out.println("The passwords do not match, please input again.");
    }
    return password0;
  }

  private byte[] inputPrivateKey() throws IOException {
    byte[] temp = new byte[128];
    byte[] result = null;
    System.out.println("Please input private key.");
    while (true) {
      int len = System.in.read(temp, 0, temp.length);
      if (len >= 64) {
        byte[] privateKey = Arrays.copyOfRange(temp, 0, 64);
        result = StringUtils.hexs2Bytes(privateKey);
        StringUtils.clear(privateKey);
        if (WalletClient.priKeyValid(result)) {
          break;
        }
      }
      StringUtils.clear(result);
      System.out.println("Invalid private key, please input again.");
    }
    StringUtils.clear(temp);
    return result;
  }

  private byte[] inputPrivateKey64() throws IOException {
    Decoder decoder = Base64.getDecoder();
    byte[] temp = new byte[128];
    byte[] result;
    System.out.println("Please input private key by base64.");
    while (true) {
      int len = System.in.read(temp, 0, temp.length);
      if (len >= 44) {
        byte[] priKey64 = Arrays.copyOfRange(temp, 0, 44);
        result = decoder.decode(priKey64);
        StringUtils.clear(priKey64);
        if (WalletClient.priKeyValid(result)) {
          break;
        }
      }
      System.out.println("Invalid base64 private key, please input again.");
    }
    StringUtils.clear(temp);
    return result;
  }

  private void registerWallet() throws CipherException, IOException {
    char[] password = new String("21e36943ae8ade91a0e6333d67d1b21f").toCharArray();//inputPassword2Twice();
    String fileName = client.registerWallet(password);
    StringUtils.clear(password);

    if (null == fileName) {
      logger.info("Register wallet failed !!");
      return;
    }
    logger.info("Register a wallet successful, keystore file name is " + fileName);
  }

  private void importWallet() throws CipherException, IOException {
    char[] password = inputPassword2Twice();
    byte[] priKey = inputPrivateKey();

    String fileName = client.importWallet(password, priKey);
    StringUtils.clear(password);
    StringUtils.clear(priKey);

    if (null == fileName) {
      System.out.println("Import wallet failed !!");
      return;
    }
    System.out.println("Import a wallet successful, keystore file name is " + fileName);
  }

  private void importwalletByBase64() throws CipherException, IOException {
    char[] password = inputPassword2Twice();
    byte[] priKey = inputPrivateKey64();

    String fileName = client.importWallet(password, priKey);
    StringUtils.clear(password);
    StringUtils.clear(priKey);

    if (null == fileName) {
      System.out.println("Import wallet failed !!");
      return;
    }
    System.out.println("Import a wallet successful, keystore file name is " + fileName);
  }

  private void changePassword() throws IOException, CipherException {
    System.out.println("Please input old password.");
    char[] oldPassword = Utils.inputPassword(false);
    System.out.println("Please input new password.");
    char[] newPassword = inputPassword2Twice();

    if (client.changePassword(oldPassword, newPassword)) {
      System.out.println("ChangePassword successful !!");
    } else {
      System.out.println("ChangePassword failed !!");
    }
    StringUtils.clear(oldPassword);
    StringUtils.clear(newPassword);
  }

  private void login() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

    boolean result = client.login(password);
    StringUtils.clear(password);

    if (result) {
      System.out.println("Login successful !!!");
    } else {
      System.out.println("Login failed !!!");
    }
  }

  private void logout() {
    client.logout();
    System.out.println("Logout successful !!!");
  }

  private void backupWallet() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

    byte[] priKey = client.backupWallet(password);
    StringUtils.clear(password);

    if (!ArrayUtils.isEmpty(priKey)) {
      System.out.println("BackupWallet successful !!");
      for (int i = 0; i < priKey.length; i++) {
        StringUtils.printOneByte(priKey[i]);
      }
      System.out.println();
    }
    StringUtils.clear(priKey);
  }

  private void backupWallet2Base64() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

    byte[] priKey = client.backupWallet(password);
    StringUtils.clear(password);

    if (!ArrayUtils.isEmpty(priKey)) {
      Encoder encoder = Base64.getEncoder();
      byte[] priKey64 = encoder.encode(priKey);
      StringUtils.clear(priKey);
      System.out.println("BackupWallet successful !!");
      for (int i = 0; i < priKey64.length; i++) {
        System.out.print((char) priKey64[i]);
      }
      System.out.println();
      StringUtils.clear(priKey64);
    }
  }

  private void getAddress() {
    String address = client.getAddress();
    if (address != null) {
      logger.info("GetAddress successful !!");
      logger.info("address = " + address);
    }
  }

  private void getBalance() {
    Account account = client.queryAccount();
    if (account == null) {
      logger.info("GetBalance failed !!!!");

    } else {
      long balance = account.getBalance();
      logger.info("Balance = " + balance);
    }
  }

  private void getAccount(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAccount needs 1 parameter like the following: ");
      System.out.println("GetAccount Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    Account account = WalletClient.queryAccount(addressBytes);
    if (account == null) {
      logger.info("GetAccount failed !!!!");
    } else {
      logger.info("\n" + Utils.printAccount(account));
    }
  }

  private void updateAccount(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("UpdateAccount needs 1 parameter like the following: ");
      System.out.println("UpdateAccount AccountName ");
      return;
    }

    String accountName = parameters[0];
    byte[] accountNameBytes = ByteArray.fromString(accountName);

    boolean ret = client.updateAccount(accountNameBytes);
    if (ret) {
      logger.info("Update Account successful !!!!");
    } else {
      logger.info("Update Account failed !!!!");
    }
  }

  private void updateAsset(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 4) {
      System.out.println("UpdateAsset needs 4 parameters like the following: ");
      System.out.println("UpdateAsset newLimit newPublicLimit description url");
      return;
    }

    String newLimitString = parameters[0];
    String newPublicLimitString = parameters[1];
    String description = parameters[2];
    String url = parameters[3];

    byte[] descriptionBytes = ByteArray.fromString(description);
    byte[] urlBytes = ByteArray.fromString(url);
    long newLimit = new Long(newLimitString);
    long newPublicLimit = new Long(newPublicLimitString);

    boolean ret = client.updateAsset(descriptionBytes, urlBytes, newLimit, newPublicLimit);
    if (ret) {
      logger.info("Update Asset successful !!!!");
    } else {
      logger.info("Update Asset failed !!!!");
    }
  }

  private void getAssetIssueByAccount(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAssetIssueByAccount needs 1 parameter like following: ");
      System.out.println("GetAssetIssueByAccount Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    Optional<AssetIssueList> result = WalletClient.getAssetIssueByAccount(addressBytes);
    if (result.isPresent()) {
      AssetIssueList assetIssueList = result.get();
      logger.info(Utils.printAssetIssueList(assetIssueList));
    } else {
      logger.info("GetAssetIssueByAccount " + " failed !!");
    }
  }

  private void getAccountNet(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAccountNet needs 1 parameter like following: ");
      System.out.println("GetAccountNet Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    AccountNetMessage result = WalletClient.getAccountNet(addressBytes);
    if (result == null) {
      logger.info("GetAccountNet " + " failed !!");
    } else {
      logger.info("\n" + Utils.printAccountNet(result));
    }
  }

  private void getAssetIssueByName(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAssetIssueByName needs 1 parameter like following: ");
      System.out.println("GetAssetIssueByName AssetName ");
      return;
    }
    String assetName = parameters[0];

    AssetIssueContract assetIssueContract = WalletClient.getAssetIssueByName(assetName);
    if (assetIssueContract != null) {
      logger.info("\n" + Utils.printAssetIssue(assetIssueContract));
    } else {
      logger.info("GetAssetIssueByName " + " failed !!");
    }
  }

  private void sendCoin(String[] parameters) throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 2) {
      System.out.println("SendCoin needs 2 parameters like following: ");
      System.out.println("SendCoin ToAddress Amount");
      return;
    }

    String toAddress = parameters[0];
    String amountStr = parameters[1];
    long amount = new Long(amountStr);

    boolean result = client.sendCoin(toAddress, amount);
    if (result) {
      logger.info("Send " + amount + " drop to " + toAddress + " successful !!");
    } else {
      logger.info("Send " + amount + " drop to " + toAddress + " failed !!");
    }
  }

  private void testTransaction(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || (parameters.length != 3 && parameters.length != 4)) {
      System.out.println("testTransaction needs 3 or 4 parameters using the following syntax: ");
      System.out.println("testTransaction ToAddress assertName times");
      System.out.println("testTransaction ToAddress assertName times interval");
      System.out.println("If needing transferAsset, assertName input null");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String loopTime = parameters[2];
    int intervalInt = 0;//s
    if (parameters.length == 5) {
      String interval = parameters[4];
      intervalInt = Integer.parseInt(interval);//s
    }
    intervalInt *= 500; //ms
    long times = new Long(loopTime);

    for (int i = 1; i <= times; i++) {
      long amount = i;
      boolean result = client.sendCoin(toAddress, amount);
      if (result) {
        logger.info("Send " + amount + " drop to " + toAddress + " successful !!");
        if (intervalInt > 0) {
          try {
            Thread.sleep(intervalInt);
          } catch (Exception e) {
            e.printStackTrace();
            break;
          }
        }
      } else {
        logger.info("Send " + amount + " drop to " + toAddress + " failed !!");
        break;
      }

      if (!"null".equalsIgnoreCase(assertName)) {
        result = client.transferAsset(toAddress, assertName, amount);
        if (result) {
          logger
              .info(
                  "transferAsset " + amount + assertName + " to " + toAddress + " successful !!");
          if (intervalInt > 0) {
            try {
              Thread.sleep(intervalInt);
            } catch (Exception e) {
              e.printStackTrace();
              break;
            }
          }
        } else {
          logger.info("transferAsset " + amount + assertName + " to " + toAddress + " failed !!");
          break;
        }
      }
    }

  }

  private void transferAsset(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 3) {
      System.out.println("TransferAsset needs 3 parameters using the following syntax: ");
      System.out.println("TransferAsset ToAddress AssertName Amount");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String amountStr = parameters[2];
    long amount = new Long(amountStr);

    boolean result = client.transferAsset(toAddress, assertName, amount);
    if (result) {
      logger.info("TransferAsset " + amount + " to " + toAddress + " successful !!");
    } else {
      logger.info("TransferAsset " + amount + " to " + toAddress + " failed !!");
    }
  }

  private void participateAssetIssue(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 3) {
      System.out.println("ParticipateAssetIssue needs 3 parameters using the following syntax: ");
      System.out.println("ParticipateAssetIssue ToAddress AssetName Amount");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String amountStr = parameters[2];
    long amount = new Integer(amountStr);

    boolean result = client.participateAssetIssue(toAddress, assertName, amount);
    if (result) {
      logger.info("ParticipateAssetIssue " + assertName + " " + amount + " from " + toAddress
          + " successful !!");
    } else {
      logger.info("ParticipateAssetIssue " + assertName + " " + amount + " from " + toAddress
          + " failed !!");
    }
  }

  private void assetIssue(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length < 10 || (parameters.length & 1) == 1) {
      System.out.println("Use the assetIssue command for features that you require with below syntax: ");
      System.out.println(
          "AssetIssue AssetName TotalSupply TrxNum AssetNum "
              + "StartDate EndDate Description Url FreeNetLimitPerAccount PublicFreeNetLimit"
              + "FrozenAmount0 FrozenDays0 ... FrozenAmountN FrozenDaysN");
      System.out
          .println(
              "TrxNum and AssetNum represents the conversion ratio of the tron to the asset.");
      System.out
          .println("The StartDate and EndDate format should look like 2018-3-1 2018-3-21 .");
      return;
    }

    String name = parameters[0];
    String totalSupplyStr = parameters[1];
    String trxNumStr = parameters[2];
    String icoNumStr = parameters[3];
    String startYyyyMmDd = parameters[4];
    String endYyyyMmDd = parameters[5];
    String description = parameters[6];
    String url = parameters[7];
    String freeNetLimitPerAccount = parameters[8];
    String publicFreeNetLimitString = parameters[9];
    HashMap<String, String> frozenSupply = new HashMap<>();
    for (int i = 10; i < parameters.length; i += 2) {
      String amount = parameters[i];
      String days = parameters[i + 1];
      frozenSupply.put(days, amount);
    }

    long totalSupply = new Long(totalSupplyStr);
    int trxNum = new Integer(trxNumStr);
    int icoNum = new Integer(icoNumStr);
    Date startDate = Utils.strToDateLong(startYyyyMmDd);
    Date endDate = Utils.strToDateLong(endYyyyMmDd);
    long startTime = startDate.getTime();
    long endTime = endDate.getTime();
    long freeAssetNetLimit = new Long(freeNetLimitPerAccount);
    long publicFreeNetLimit = new Long(publicFreeNetLimitString);

    boolean result = client
        .assetIssue(name, totalSupply, trxNum, icoNum, startTime, endTime,
            0, description, url, freeAssetNetLimit, publicFreeNetLimit, frozenSupply);
    if (result) {
      logger.info("AssetIssue " + name + " successful !!");
    } else {
      logger.info("AssetIssue " + name + " failed !!");
    }
  }

  private void createAccount(String[] parameters)
      throws CipherException, IOException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("CreateAccount needs 1 parameter using the following syntax: ");
      System.out.println("CreateAccount Address");
      return;
    }

    String address = parameters[0];

    boolean result = client.createAccount(address);
    if (result) {
      logger.info("CreateAccount " + " successful !!");
    } else {
      logger.info("CreateAccount " + " failed !!");
    }
  }

  private void createWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("CreateWitness needs 1 parameter using the following syntax: ");
      System.out.println("CreateWitness Url");
      return;
    }

    String url = parameters[0];

    boolean result = client.createWitness(url);
    if (result) {
      logger.info("CreateWitness " + " successful !!");
    } else {
      logger.info("CreateWitness " + " failed !!");
    }
  }

  private void updateWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("updateWitness needs 1 parameter using the following syntax: ");
      System.out.println("updateWitness Url");
      return;
    }

    String url = parameters[0];

    boolean result = client.updateWitness(url);
    if (result) {
      logger.info("updateWitness " + " successful !!");
    } else {
      logger.info("updateWitness " + " failed !!");
    }
  }

  private void listWitnesses() {
    Optional<WitnessList> result = client.listWitnesses();
    if (result.isPresent()) {
      WitnessList witnessList = result.get();
      logger.info(Utils.printWitnessList(witnessList));
    } else {
      logger.info("List witnesses " + " failed !!");
    }
  }

  private void getAssetIssueList() {
    Optional<AssetIssueList> result = client.getAssetIssueList();
    if (result.isPresent()) {
      AssetIssueList assetIssueList = result.get();
      logger.info(Utils.printAssetIssueList(assetIssueList));
    } else {
      logger.info("GetAssetIssueList " + " failed !!");
    }
  }

  private void getAssetIssueList(String[] parameters) {
    if (parameters == null || parameters.length != 2) {
      System.out.println("The listassetissuepaginated command needs 2 parameters, use the following syntax:");
      System.out.println("listassetissuepaginated offset limit ");
      return;
    }
    int offset = Integer.parseInt(parameters[0]);
    int limit = Integer.parseInt(parameters[1]);
    Optional<AssetIssueList> result = client.getAssetIssueList(offset, limit);
    if (result.isPresent()) {
      AssetIssueList assetIssueList = result.get();
      logger.info(Utils.printAssetIssueList(assetIssueList));
    } else {
      logger.info("GetAssetIssueListPaginated " + " failed !!");
    }
  }

  private void listNodes() {
    Optional<NodeList> result = client.listNodes();
    if (result.isPresent()) {
      NodeList nodeList = result.get();
      List<Node> list = nodeList.getNodesList();
      for (int i = 0; i < list.size(); i++) {
        Node node = list.get(i);
        logger.info("IP::" + ByteArray.toStr(node.getAddress().getHost().toByteArray()));
        logger.info("Port::" + node.getAddress().getPort());
      }
    } else {
      logger.info("GetAssetIssueList " + " failed !!");
    }
  }

  private void GetBlock(String[] parameters) {
    long blockNum = -1;

    if (parameters == null || parameters.length == 0) {
      System.out.println("Get current block !!!!");
    } else {
      if (parameters.length != 1) {
        System.out.println("Getblock has too many parameters !!!");
        System.out.println("You can get current block using the following command:");
        System.out.println("Getblock");
        System.out.println("Or get block by number with the following syntax:");
        System.out.println("Getblock BlockNum");
      }
      blockNum = Long.parseLong(parameters[0]);
    }
    Block block = client.getBlock(blockNum);
    if (block == null) {
      logger.info("No block for num : " + blockNum);
      return;
    }
    logger.info(Utils.printBlock(block));
  }

  private void voteWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length < 2 || (parameters.length & 1) != 0) {
      System.out.println("Use VoteWitness command with below syntax: ");
      System.out.println("VoteWitness Address0 Count0 ... AddressN CountN");
      return;
    }

    HashMap<String, String> witness = new HashMap<String, String>();
    for (int i = 0; i < parameters.length; i += 2) {
      String address = parameters[i];
      String countStr = parameters[i + 1];
      witness.put(address, countStr);
    }

    boolean result = client.voteWitness(witness);
    if (result) {
      logger.info("VoteWitness " + " successful !!");
    } else {
      logger.info("VoteWitness " + " failed !!");
    }
  }

  private void freezeBalance(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 2) {
      System.out.println("Use freezeBalance command with below syntax: ");
      System.out.println("freezeBalance frozen_balance frozen_duration ");
      return;
    }

    long frozen_balance = Long.parseLong(parameters[0]);
    long frozen_duration = Long.parseLong(parameters[1]);

    boolean result = client.freezeBalance(frozen_balance, frozen_duration);
    if (result) {
      logger.info("freezeBalance " + " successful !!");
    } else {
      logger.info("freezeBalance " + " failed !!");
    }
  }

  private void unfreezeBalance()
      throws IOException, CipherException, CancelException {
    boolean result = client.unfreezeBalance();
    if (result) {
      logger.info("unfreezeBalance " + " successful !!");
    } else {
      logger.info("unfreezeBalance " + " failed !!");
    }
  }

  private void unfreezeAsset() throws IOException, CipherException, CancelException {
    boolean result = client.unfreezeAsset();
    if (result) {
      logger.info("unfreezeAsset " + " successful !!");
    } else {
      logger.info("unfreezeAsset " + " failed !!");
    }
  }

  private void withdrawBalance() throws IOException, CipherException, CancelException {
    boolean result = client.withdrawBalance();
    if (result) {
      logger.info("withdrawBalance " + " successful !!");
    } else {
      logger.info("withdrawBalance " + " failed !!");
    }
  }

  private void getTotalTransaction() {
    NumberMessage totalTransition = client.getTotalTransaction();
    logger.info("The num of total transactions is : " + totalTransition.getNum());
  }

  private void getNextMaintenanceTime() {
    NumberMessage nextMaintenanceTime = client.getNextMaintenanceTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String date = formatter.format(nextMaintenanceTime.getNum());
    logger.info("Next maintenance time is : " + date);
  }

//  private void getAssetIssueListByTimestamp(String[] parameters) {
//    long timeStamp = -1;
//    if (parameters == null || parameters.length == 0) {
//      System.out.println("no time input, use current time");
//      timeStamp = System.currentTimeMillis();
//    } else {
//      if (parameters.length != 2) {
//        System.out.println("You can GetAssetIssueListByTimestamp like:");
//        System.out.println("GetAssetIssueListByTimestamp yyyy-mm-dd hh:mm:ss");
//        return;
//      } else {
//        timeStamp = Timestamp.valueOf(parameters[0] + " " + parameters[1]).getTime();
//      }
//    }
//    Optional<AssetIssueList> result = WalletClient.getAssetIssueListByTimestamp(timeStamp);
//    if (result.isPresent()) {
//      AssetIssueList assetIssueList = result.get();
//      logger.info(Utils.printAssetIssueList(assetIssueList));
//    } else {
//      logger.info("GetAssetIssueListByTimestamp " + " failed !!");
//    }
//  }

//  private void getTransactionsByTimestamp(String[] parameters) {
//    String start = "";
//    String end = "";
//    if (parameters == null || parameters.length != 6) {
//      System.out.println(
//          "getTransactionsByTimestamp needs 4 parameters, start_time and end_time, time format is yyyy-mm-dd hh:mm:ss, offset and limit");
//      return;
//    } else {
//      start = parameters[0] + " " + parameters[1];
//      end = parameters[2] + " " + parameters[3];
//    }
//    long startTime = Timestamp.valueOf(start).getTime();
//    long endTime = Timestamp.valueOf(end).getTime();
//    int offset = Integer.parseInt(parameters[4]);
//    int limit = Integer.parseInt(parameters[5]);
//    Optional<TransactionList> result = WalletClient
//        .getTransactionsByTimestamp(startTime, endTime, offset, limit);
//    if (result.isPresent()) {
//      TransactionList transactionList = result.get();
//      logger.info(Utils.printTransactionList(transactionList));
//    } else {
//      logger.info("getTransactionsByTimestamp " + " failed !!");
//    }
//  }

//  private void getTransactionsByTimestampCount(String[] parameters) {
//    String start = "";
//    String end = "";
//    if (parameters == null || parameters.length != 4) {
//      System.out.println(
//          "getTransactionsByTimestampCount needs 2 parameters, start_time and end_time, time format is yyyy-mm-dd hh:mm:ss");
//      return;
//    } else {
//      start = parameters[0] + " " + parameters[1];
//      end = parameters[2] + " " + parameters[3];
//    }
//    long startTime = Timestamp.valueOf(start).getTime();
//    long endTime = Timestamp.valueOf(end).getTime();
//
//    NumberMessage result = WalletClient.getTransactionsByTimestampCount(startTime, endTime);
//    logger.info("the number of Transactions from " + start + " to " + end + " is " + result);
//  }

  private void getTransactionById(String[] parameters) {
    String txid = "";
    if (parameters == null || parameters.length != 1) {
      System.out.println("getTransactionById needs 1 parameter, transaction id");
      return;
    } else {
      txid = parameters[0];
    }
    Optional<Transaction> result = WalletClient.getTransactionById(txid);
    if (result.isPresent()) {
      Transaction transaction = result.get();
      logger.info(Utils.printTransaction(transaction));
    } else {
      logger.info("getTransactionById " + " failed !!");
    }
  }

  private void getTransactionsFromThis(String[] parameters) {
    if (parameters == null || parameters.length != 3) {
      System.out.println("GetTransactionsFromThis needs 3 parameters, use the following syntax: ");
      System.out.println("GetTransactionsFromThis Address offset limit");
      return;
    }
    String address = parameters[0];
    int offset = Integer.parseInt(parameters[1]);
    int limit = Integer.parseInt(parameters[2]);
    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    Optional<TransactionList> result = WalletClient
        .getTransactionsFromThis(addressBytes, offset, limit);
    if (result.isPresent()) {
      TransactionList transactionList = result.get();
      logger.info(Utils.printTransactionList(transactionList));
    } else {
      logger.info("GetTransactionsFromThis " + " failed !!");
    }
  }

//  private void getTransactionsFromThisCount(String[] parameters) {
//    if (parameters == null || parameters.length != 1) {
//      System.out.println("getTransactionsFromThisCount need 1 parameter like following: ");
//      System.out.println("getTransactionsFromThisCount Address");
//      return;
//    }
//    String address = parameters[0];
//    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
//    if (addressBytes == null) {
//      return;
//    }
//
//    NumberMessage result = WalletClient.getTransactionsFromThisCount(addressBytes);
//    logger.info("the number of Transactions from account " + address + " is " + result);
//  }

  private void getTransactionsToThis(String[] parameters) {
    if (parameters == null || parameters.length != 3) {
      System.out.println("getTransactionsToThis needs 3 parameters, use the following syntax: ");
      System.out.println("getTransactionsToThis Address offset limit");
      return;
    }
    String address = parameters[0];
    int offset = Integer.parseInt(parameters[1]);
    int limit = Integer.parseInt(parameters[2]);
    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    Optional<TransactionList> result = WalletClient
        .getTransactionsToThis(addressBytes, offset, limit);
    if (result.isPresent()) {
      TransactionList transactionList = result.get();
      logger.info(Utils.printTransactionList(transactionList));
    } else {
      logger.info("getTransactionsToThis " + " failed !!");
    }
  }

//  private void getTransactionsToThisCount(String[] parameters) {
//    if (parameters == null || parameters.length != 1) {
//      System.out.println("getTransactionsToThisCount need 1 parameter like following: ");
//      System.out.println("getTransactionsToThisCount Address");
//      return;
//    }
//    String address = parameters[0];
//    byte[] addressBytes = WalletClient.decodeFromBase58Check(address);
//    if (addressBytes == null) {
//      return;
//    }
//
//    NumberMessage result = WalletClient.getTransactionsToThisCount(addressBytes);
//    logger.info("the number of Transactions to account " + address + " is " + result);
//  }

  private void getBlockById(String[] parameters) {
    String blockID = "";
    if (parameters == null || parameters.length != 1) {
      System.out.println("getBlockById needs 1 parameter, block id which is hex format");
      return;
    } else {
      blockID = parameters[0];
    }
    Optional<Block> result = WalletClient.getBlockById(blockID);
    if (result.isPresent()) {
      Block block = result.get();
      logger.info(Utils.printBlock(block));
    } else {
      logger.info("getBlockById " + " failed !!");
    }
  }

  private void getBlockByLimitNext(String[] parameters) {
    long start = 0;
    long end = 0;
    if (parameters == null || parameters.length != 2) {
      System.out
          .println("GetBlockByLimitNext needs 2 parameters, start block id and end block id");
      return;
    } else {
      start = Long.parseLong(parameters[0]);
      end = Long.parseLong(parameters[1]);
    }
    Optional<BlockList> result = WalletClient.getBlockByLimitNext(start, end);
    if (result.isPresent()) {
      BlockList blockList = result.get();
      logger.info(Utils.printBlockList(blockList));
    } else {
      logger.info("GetBlockByLimitNext " + " failed !!");
    }
  }

  private void getBlockByLatestNum(String[] parameters) {
    long num = 0;
    if (parameters == null || parameters.length != 1) {
      System.out.println("getBlockByLatestNum needs 1 parameter, block num");
      return;
    } else {
      num = Long.parseLong(parameters[0]);
    }
    Optional<BlockList> result = WalletClient.getBlockByLatestNum(num);
    if (result.isPresent()) {
      BlockList blockList = result.get();
      logger.info(Utils.printBlockList(blockList));
    } else {
      logger.info("getBlockByLatestNum " + " failed !!");
    }
  }

  private void help() {
    System.out.println("Help: List of Tron Wallet-cli commands");
    System.out.println("For more information on a specific command, type the command and it will display tips");
    System.out.println("");
    System.out.println("RegisterWallet");
    System.out.println("ImportWallet");
    System.out.println("ImportWalletByBase64");
    System.out.println("ChangePassword");
    System.out.println("Login");
    System.out.println("Logout");
    System.out.println("BackupWallet");
    System.out.println("BackupWallet2Base64");
    System.out.println("GetAddress");
    System.out.println("GetBalance");
    System.out.println("GetAccount");
    System.out.println("GetAssetIssueByAccount");
    System.out.println("GetAccountNet");
    System.out.println("GetAssetIssueByName");
    System.out.println("SendCoin");
    System.out.println("TransferAsset");
    System.out.println("ParticipateAssetIssue");
    System.out.println("AssetIssue");
    System.out.println("CreateAccount");
    System.out.println("CreateWitness");
    System.out.println("UpdateWitness");
    System.out.println("VoteWitness");
    System.out.println("ListWitnesses");
    System.out.println("ListAssetIssue");
    System.out.println("ListNodes");
    System.out.println("GetBlock");
    System.out.println("GetTotalTransaction");
    //   System.out.println("GetAssetIssueListByTimestamp");
    System.out.println("GetTotalTransaction");
    System.out.println("GetNextMaintenanceTime");
    //   System.out.println("GetTransactionsByTimestamp");
    //   System.out.println("GetTransactionsByTimestampCount");
    System.out.println("GetTransactionById");
    System.out.println("GetTransactionsFromThis");
    //   System.out.println("GetTransactionsFromThisCount");
    System.out.println("GetTransactionsToThis");
    //   System.out.println("GetTransactionsToThisCount");
    System.out.println("GetBlockById");
    System.out.println("GetBlockByLimitNext");
    System.out.println("GetBlockByLatestNum");
    System.out.println("FreezeBalance");
    System.out.println("UnfreezeBalance");
    System.out.println("WithdrawBalance");
    System.out.println("UpdateAccount");
    System.out.println("UpdateAsset");
    System.out.println("UnfreezeAsset");
    System.out.println("Exit or Quit");

    System.out.println("Input any one of the listed commands, to display how-to tips.");
  }

  private void run() {
    Scanner in = new Scanner(System.in);
    System.out.println(" ");
    System.out.println("Welcome to Tron Wallet-Cli");
    System.out.println("Please type one of the following commands to proceed.");
    System.out.println("Login, RegisterWallet or ImportWallet");
    System.out.println(" ");
    System.out.println("You may also use the Help command at anytime to display a full list of commands.");
    System.out.println(" ");
    while (in.hasNextLine()) {
      String cmd = "";
      try {
        String cmdLine = in.nextLine().trim();
        String[] cmdArray = cmdLine.split("\\s+");
        // split on trim() string will always return at the minimum: [""]
        cmd = cmdArray[0];
        if ("".equals(cmd)) {
          continue;
        }
        String[] parameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);
        String cmdLowerCase = cmd.toLowerCase();

        switch (cmdLowerCase) {
          case "help": {
            help();
            break;
          }
          case "registerwallet": {
            for (int i = 0; i < 30; i++) {
              registerWallet();
            }
            break;
          }
          case "importwallet": {
            importWallet();
            break;
          }
          case "importwalletbybase64": {
            importwalletByBase64();
            break;
          }
          case "changepassword": {
            changePassword();
            break;
          }
          case "login": {
            login();
            break;
          }
          case "logout": {
            logout();
            break;
          }
          case "backupwallet": {
            backupWallet();
            break;
          }
          case "backupwallet2base64": {
            backupWallet2Base64();
            break;
          }
          case "getaddress": {
            getAddress();
            break;
          }
          case "getbalance": {
            getBalance();
            break;
          }
          case "getaccount": {
            getAccount(parameters);
            break;
          }
          case "updateaccount": {
            updateAccount(parameters);
            break;
          }
          case "updateasset": {
            updateAsset(parameters);
            break;
          }
          case "getassetissuebyaccount": {
            getAssetIssueByAccount(parameters);
            break;
          }
          case "getaccountnet": {
            getAccountNet(parameters);
            break;
          }
          case "getassetissuebyname": {
            getAssetIssueByName(parameters);
            break;
          }
          case "sendcoin": {
            sendCoin(parameters);
            break;
          }
          case "testtransaction": {
            testTransaction(parameters);
            break;
          }
          case "transferasset": {
            transferAsset(parameters);
            break;
          }
          case "participateassetissue": {
            participateAssetIssue(parameters);
            break;
          }
          case "assetissue": {
            assetIssue(parameters);
            break;
          }
          case "createaccount": {
            createAccount(parameters);
            break;
          }
          case "createwitness": {
            createWitness(parameters);
            break;
          }
          case "updatewitness": {
            updateWitness(parameters);
            break;
          }
          case "votewitness": {
            voteWitness(parameters);
            break;
          }
          case "freezebalance": {
            freezeBalance(parameters);
            break;
          }
          case "unfreezebalance": {
            unfreezeBalance();
            break;
          }
          case "unfreezeasset": {
            unfreezeAsset();
            break;
          }
          case "withdrawbalance": {
            withdrawBalance();
            break;
          }
          case "listwitnesses": {
            listWitnesses();
            break;
          }
          case "listassetissue": {
            getAssetIssueList();
            break;
          }
          case "listassetissuepaginated": {
            getAssetIssueList(parameters);
            break;
          }
          case "listnodes": {
            listNodes();
            break;
          }
          case "getblock": {
            GetBlock(parameters);
            break;
          }
          case "gettotaltransaction": {
            getTotalTransaction();
            break;
          }
          case "getnextmaintenancetime": {
            getNextMaintenanceTime();
            break;
          }
//          case "getassetissuelistbytimestamp": {
//            getAssetIssueListByTimestamp(parameters);
//            break;
//          }
//          case "gettransactionsbytimestampcount": {
//            getTransactionsByTimestampCount(parameters);
//            break;
//          }
          case "gettransactionsfromthis": {
            getTransactionsFromThis(parameters);
            break;
          }
//          case "gettransactionsfromthiscount": {
//            getTransactionsFromThisCount(parameters);
//            break;
//          }
          case "gettransactionstothis": {
            getTransactionsToThis(parameters);
            break;
          }
//          case "gettransactionstothiscount": {
//            getTransactionsToThisCount(parameters);
//            break;
//          }
//          case "gettransactionsbytimestamp": {
//            getTransactionsByTimestamp(parameters);
//            break;
//          }
          case "gettransactionbyid": {
            getTransactionById(parameters);
            break;
          }
          case "getblockbyid": {
            getBlockById(parameters);
            break;
          }
          case "getblockbylimitnext": {
            getBlockByLimitNext(parameters);
            break;
          }
          case "getblockbylatestnum": {
            getBlockByLatestNum(parameters);
            break;
          }
          case "exit":
          case "quit": {
            System.out.println("Exit !!!");
            return;
          }
          default: {
            System.out.println("Invalid cmd: " + cmd);
            help();
          }
        }
      } catch (CipherException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (IOException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (CancelException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (Exception e) {
        System.out.println(cmd + " failed!");
        logger.error(e.getMessage());
      }
    }
  }

  public static void main(String[] args) {
    TestClient cli = new TestClient();

    JCommander.newBuilder()
        .addObject(cli)
        .build()
        .parse(args);

    cli.run();
  }
}
