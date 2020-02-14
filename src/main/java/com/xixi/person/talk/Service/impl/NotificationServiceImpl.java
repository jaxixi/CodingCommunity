package com.xixi.person.talk.Service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xixi.person.talk.enums.NotificationEnum;
import com.xixi.person.talk.Service.NotificationService;
import com.xixi.person.talk.dto.NotificationDto;
import com.xixi.person.talk.exception.QuestionErrorCodeEnum;
import com.xixi.person.talk.exception.QuestionException;
import com.xixi.person.talk.Mapper.NotificationMapper;
import com.xixi.person.talk.Mapper.UserMapper;
import com.xixi.person.talk.Model.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: xixi-98
 * @Date: 2019/12/29 17:52
 * @Description:
 */
@Service
public class NotificationServiceImpl implements NotificationService {
    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public  PageInfo selNotificationList(Long accountId, int size, int page) {

        //查询出当前页回复
        PageHelper.startPage(page,size);
        NotificationExample notificationExample=new NotificationExample();
        notificationExample.setOrderByClause("gmt_create desc");
        //判断accountId是否存在
        if(accountId != 0L){
            notificationExample.createCriteria().andReceiverEqualTo(accountId);
        }
        //回复总数
        List<Notification> notifications = notificationMapper.selectByExample(notificationExample);
        //查询出当前页问题，navugatepage是导航页
        PageInfo pageInfo=new PageInfo(notifications,5);
        List<NotificationDto> list = new ArrayList();
        List pageInfoList = pageInfo.getList();
        for (Object o : pageInfoList) {
            NotificationDto notificationDto=new NotificationDto();
            BeanUtils.copyProperties(o,notificationDto);
            Notification source = (Notification) o;
            notificationDto.setTypeName(NotificationEnum.getName(source.getType()));
            list.add(notificationDto);
        }
        pageInfo.setList(list);
        return pageInfo;

    }

    @Override
    public int unreadCount(Long id) {
        NotificationExample example = new NotificationExample();
        example.createCriteria().andNotifierEqualTo(id).andStatusEqualTo(NotificationEnum.UEREAD.getType());
        return notificationMapper.countByExample(example);
    }

    @Override
    public Notification read(Long id, User user) {
        Notification notification = notificationMapper.selectByPrimaryKey(id);
        if(notification==null){
            throw  new QuestionException(QuestionErrorCodeEnum.READ_NOTIFICATION_FAIL);
        }
        if (!notification.getReceiver().equals(user.getAccountId())){
            throw  new QuestionException(QuestionErrorCodeEnum.NOTIFICATION_NOT_FOUND);
        }
        notification.setStatus(NotificationEnum.READ.getType());
        notificationMapper.updateByPrimaryKey(notification);
        Integer unreadCount = (Integer) redisTemplate.opsForValue().get("unreadCount");
        if (unreadCount == null){
            unreadCount = this.unreadCount(notification.getReceiver());
            redisTemplate.opsForValue().set("unreadCount",unreadCount);
        }
        redisTemplate.opsForValue().set("unreadCount",unreadCount-1);
        return notification;
    }
}
