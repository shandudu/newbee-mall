package ltd.newbee.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ltd.newbee.mall.dao.SeckillDao;
import ltd.newbee.mall.entity.*;
import ltd.newbee.mall.entity.vo.MallUserVO;
import ltd.newbee.mall.entity.vo.SeckillVO;
import ltd.newbee.mall.exception.BusinessException;
import ltd.newbee.mall.service.*;
import ltd.newbee.mall.util.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillDao, Seckill> implements SeckillService {

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SeckillSuccessService seckillSuccessService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private ShopCatService shopCatService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;


    @Override
    public IPage selectPage(Page<Seckill> page, SeckillVO seckillVO) {
        return seckillDao.selectListPage(page, seckillVO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String executeSeckill(Long seckillId, MallUserVO userVO) {
        Seckill seckill = getById(seckillId);
        Goods goods = goodsService.getById(seckill.getGoodsId());
        int count = seckillSuccessService.count(new QueryWrapper<SeckillSuccess>()
                .eq("seckill_id", seckillId)
                .eq("user_id", userVO.getUserId()));
        if (count >= seckill.getLimitNum()) {
            throw new BusinessException("用户购买数量有已经超出秒杀限购数量");
        }
        if (seckill.getSeckillNum() <= 0) {
            throw new BusinessException("秒杀商品已售空");
        }
        // 执行秒杀逻辑：减库存 + 记录购买行为
        Date now = new Date();
        if (!seckillDao.reduceNumber(seckillId, now.getTime() / 1000)) {
            throw new BusinessException("秒杀商品减库存失败");
        }
        SeckillSuccess seckillSuccess = new SeckillSuccess();
        seckillSuccess.setSeckillId(seckillId);
        seckillSuccess.setUserId(userVO.getUserId());
        if (!seckillSuccessService.save(seckillSuccess)) {
            throw new BusinessException("保存用户秒杀商品失败");
        }
        // 生成订单号
        String orderNo = NumberUtil.genOrderNo();
        // 保存订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setTotalPrice(seckill.getSeckillPrice());
        order.setUserId(userVO.getUserId());
        order.setUserAddress(userVO.getAddress());
        String extraInfo = "";
        order.setExtraInfo(extraInfo);
        if (!orderService.save(order)) {
            throw new BusinessException("生成订单内部异常");
        }
        // 保存订单商品项
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getOrderId());
        orderItem.setGoodsId(goods.getGoodsId());
        orderItem.setGoodsCoverImg(goods.getGoodsCoverImg());
        orderItem.setGoodsName(goods.getGoodsName());
        orderItem.setGoodsCount(1);
        orderItem.setSellingPrice(seckill.getSeckillPrice());
        if (!orderItemService.save(orderItem)) {
            throw new BusinessException("生成订单内部异常");
        }
        return orderNo;
    }
}
