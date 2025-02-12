package ltd.newbee.mall.core.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ltd.newbee.mall.core.dao.CouponUserDao;
import ltd.newbee.mall.core.entity.Coupon;
import ltd.newbee.mall.core.entity.CouponUser;
import ltd.newbee.mall.exception.BusinessException;
import ltd.newbee.mall.core.service.CouponService;
import ltd.newbee.mall.core.service.CouponUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
public class CouponUserServiceImpl extends ServiceImpl<CouponUserDao, CouponUser> implements CouponUserService {

    @Autowired
    private CouponService couponService;

    @Override
    public boolean saveCouponUser(Long couponId, Long userId) {
        Coupon coupon = couponService.getById(couponId);
        if (coupon.getCouponLimit() != 0) {
            int count = count(new QueryWrapper<CouponUser>()
                    .eq("user_id", userId)
                    .eq("coupon_id", coupon.getCouponId()));
            if (count != 0) {
                throw new BusinessException("优惠卷已经领过了,无法再次领取！");
            }
        }
        if (coupon.getCouponTotal() != 0) {
            int count = count(new QueryWrapper<CouponUser>()
                    .eq("coupon_id", coupon.getCouponId()));
            if (count >= coupon.getCouponTotal()) {
                throw new BusinessException("优惠卷已经领完了！");
            }
        }
        CouponUser couponUser = new CouponUser();
        couponUser.setUserId(userId);
        couponUser.setCouponId(coupon.getCouponId());
        LocalDate startLocalDate = LocalDate.now();
        LocalDate endLocalDate = startLocalDate.plusDays(coupon.getDays());
        ZoneId zone = ZoneId.systemDefault();
        Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(zone).toInstant());
        Date endDate = Date.from(endLocalDate.atStartOfDay().atZone(zone).toInstant());
        couponUser.setStartTime(startDate);
        couponUser.setEndTime(endDate);
        couponUser.setCreateTime(new Date());
        return save(couponUser);
    }

    @Override
    public Coupon getCoupon(Long orderId) {
        CouponUser couponUser = getOne(new QueryWrapper<CouponUser>().eq("order_id", orderId));
        if (couponUser == null) {
            return null;
        }
        return couponService.getById(couponUser.getCouponId());
    }
}
