package com.personal.oyl.event.sample.subscribers;

import com.personal.oyl.event.EventSerde;
import com.personal.oyl.event.sample.order.Order;
import com.personal.oyl.event.sample.order.OrderRepos;
import com.personal.oyl.event.sample.order.UserOrderReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.personal.oyl.event.EventSubscriber;
import com.personal.oyl.event.Event;
import com.personal.oyl.event.EventMapper;

import javax.annotation.Resource;

/**
 * @author OuYang Liang
 */
@Component("userOrderReportSubscriber")
public class UserOrderReportSubscriber implements EventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(UserOrderReportSubscriber.class);
    
    @Resource
    private OrderRepos orderRepos;
    
    @Resource
    private EventMapper eventMapper;

    @Resource
    private EventSerde eventSerde;
    
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @Override
    public void onEvent(Event e) {
        
        try {
            Order order = Order.fromJson(e.getContext());
            UserOrderReport report = orderRepos.selectUserOrderReportByKey(order.getUserId());
            
            if (null == report) {
                report = new UserOrderReport();
                report.setUserId(order.getUserId());
                report.setOrderNum(1L);
                report.setOrderTotal(new Long(order.getOrderAmount()));

                orderRepos.createUserOrderReport(report);
            } else {
                report.setOrderNum(report.getOrderNum() + 1);
                report.setOrderTotal(report.getOrderTotal() + order.getOrderAmount());

                orderRepos.updateUserOrderReport(report);
            }
            
            eventMapper.archive(this.id(), e);
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicated message " + eventSerde.toJson(e));
        }
        
    }

    @Override
    public String id() {
        return "000000000001";
    }

}
