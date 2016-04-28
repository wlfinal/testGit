package test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import redis.clients.jedis.Jedis;
import cn.com.bsfit.frms.ds.pojo.DSPayOrder;
import cn.com.bsfit.frms.ds.pojo.DSSigned;
import cn.com.bsfit.frms.ds.pojo.DSUsers;
import cn.com.bsfit.frms.obj.AuditObject;
import cn.com.bsfit.frms.obj.CountNumber;
import cn.com.bsfit.frms.obj.MaxDecreaseCountNumber;
import cn.com.bsfit.frms.obj.MemCachedItem;
import cn.com.bsfit.frms.obj.Mergeable;
import cn.com.bsfit.frms.obj.MergeableListObject;
import cn.com.bsfit.frms.obj.SumNumber;
import cn.com.bsfit.frms.obj.TimedItems;
import cn.com.bsfit.frms.serial.MemCachedItemUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class TestCondition {
	private Logger logger = LoggerFactory.getLogger(TestCondition.class);
//2016-4-12 13:00:00
	String ashost = "192.168.42.130";
	String hqhost = "192.168.42.130";
	
	public static void main(String[] args) throws Exception{
		TestCondition test = new TestCondition();
		test.insertPayOrder();
		Thread.sleep(200);
		MemCachedItem cache = test.getCacheFromAS("card-2", "卡号", "PAY.BUY", true);
		System.out.println(cache);
             tPayCustCardNo("card-1");
		List<DSPayOrder> orders = new ArrayList<DSPayOrder>();
		orders.add(payOrder);
		sendToPreQueue(orders);
	}
	
	private void insertSigned() throws NamingException, JMSException, ParseException{
		Date time = getDate("2016-4-12 13:00:00");
		DSSigned sign = new DSSigned();
		sign.setUserId("uid-1");
		sign.setSignedTime(time);
		List<DSSigned> signs = new ArrayList<DSSigned>();
		signs.add(sign);
		sendToPreQueue(signs);
	}
	
	private void insertUser() throws ParseException, NamingException, JMSException{
		Date time = getDate("2016-4-12 13:00:00");
		DSUsers user = new DSUsers();
		user.setUserId("uid-2");
		user.setRegisterTime(time);
		user.setCertificatePrv("海南");
		user.setPhonePrv("北京");
		List<DSUsers> users = new ArrayList<DSUsers>();
		users.add(user);
		
		sendToPreQueue(users);
	}
	
	private long testCondition(Object cache,String pattern,Object transTimeObj) {
		    if (cache == null)
		        return 0;
		    TimedItems timedItems = (TimedItems) cache;
		    long transTime = (Long) transTimeObj;
		    Object resultObj = timedItems.getRaw(transTime, pattern);
		    if (resultObj != null) {
		        if (resultObj instanceof SumNumber) {
		            SumNumber sn = (SumNumber) resultObj;
		            return sn.getCount();
		        }
		        if (resultObj instanceof CountNumber) {
		            CountNumber cn = (CountNumber) resultObj;
		            System.out.println(cn.getCount());
		            return cn.getCount();
		        }
		        if (resultObj instanceof MaxDecreaseCountNumber) {
		            MaxDecreaseCountNumber cn = (MaxDecreaseCountNumber) resultObj;
		            return cn.getCount();
		        }            
		    }
		    return 0;
	}
	private Date getDate(String str) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.parse(str);
	}
	
	private MemCachedItem getCacheFromAS(String pk,String tag,String biz,boolean timeout){
		AerospikeClient client = new AerospikeClient(ashost, 3000);
		Key key = new Key("bsfit", "frms", MemCachedItem.getMemCachedKey(pk, tag, biz, timeout));
		Record record = client.get(null, key);
		MemCachedItem item = MemCachedItemUtils.asMemCachedItem(record,kryo());
		client.close();
		return item;
	}
	
	private boolean delCacheFromAS(String pk,String tag,String biz,boolean timeout){
		AerospikeClient client = new AerospikeClient(ashost, 3000);
		Key key = new Key("bsfit", "frms", MemCachedItem.getMemCachedKey(pk, tag, biz, timeout));
		boolean a=client.delete(null, key);
		if(a)
			System.out.println("缓存清空成功！！！");
		else
			System.out.println("缓存清空失败！！！");
		client.close();
		return a;
	}
	
	private void sendToPreQueue(List<?> lists) throws NamingException, JMSException{
		Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        props.setProperty("java.naming.provider.url", hqhost+":1099");
        InitialContext ic = new InitialContext(props);
		ConnectionFactory cf = (ConnectionFactory) ic.lookup("/ConnectionFactory");
		String queueName = "PreDSQueue";
		Queue queue = (Queue)ic.lookup("/queue/" + queueName);
		Connection conn = cf.createConnection();
		Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageProducer producer = sess.createProducer(queue);
		try{
			String text = JSON.toJSONString(lists, SerializerFeature.BrowserCompatible,
	                SerializerFeature.WriteClassName, SerializerFeature.DisableCircularReferenceDetect);
			System.out.println(text);
			TextMessage tm = sess.createTextMessage(text);
			producer.send(tm);
			System.out.println("---------------------队列插入数据完毕！---------------------");
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			sess.close();
			conn.close();
		}
	}
	
	private Kryo kryo(){
    	KryoFactory factory = new KryoFactory() {
            public Kryo create() {
                Kryo kryo = new Kryo();
                return kryo;
            }
        };
        KryoPool pool = new KryoPool.Builder(factory).softReferences().build();
        return pool.borrow();
    }
	
}