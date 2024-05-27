package jp.co.metateam.library.service;

import java.sql.Timestamp;
import java.util.List;

//import javax.xml.crypto.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.ui.Model;

//import io.micrometer.common.util.StringUtils;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.repository.AccountRepository;
import jp.co.metateam.library.repository.RentalManageRepository;
import jp.co.metateam.library.repository.StockRepository;
import jp.co.metateam.library.values.RentalStatus;
import lombok.extern.log4j.Log4j2;


@Log4j2
@Service
public class RentalManageService {

    private final AccountRepository accountRepository;
    private final RentalManageRepository rentalManageRepository;
    private final StockRepository stockRepository;

     @Autowired
    public RentalManageService(
        AccountRepository accountRepository,
        RentalManageRepository rentalManageRepository,
        StockRepository stockRepository
    ) {
        this.accountRepository = accountRepository;
        this.rentalManageRepository = rentalManageRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public List <RentalManage> findAll() {
        List <RentalManage> rentalManageList = this.rentalManageRepository.findAll();

        return rentalManageList;
    }

    @Transactional
    public RentalManage findById(Long id) {
        return this.rentalManageRepository.findById(id).orElse(null);
    }

    @Transactional 
    public void save(RentalManageDto rentalManageDto) throws Exception {
        try {
            Account account = this.accountRepository.findByEmployeeId(rentalManageDto.getEmployeeId()).orElse(null);
            if (account == null) {
                throw new Exception("Account not found.");
            }

            Stock stock = this.stockRepository.findById(rentalManageDto.getStockId()).orElse(null);
            if (stock == null) {
                throw new Exception("Stock not found.");
            }

            RentalManage rentalManage = new RentalManage();
            rentalManage = setRentalStatusDate(rentalManage, rentalManageDto.getStatus());

            rentalManage.setAccount(account);
            rentalManage.setExpectedRentalOn(rentalManageDto.getExpectedRentalOn());
            rentalManage.setExpectedReturnOn(rentalManageDto.getExpectedReturnOn());
            rentalManage.setStatus(rentalManageDto.getStatus());
            rentalManage.setStock(stock);

            // データベースへの保存
            this.rentalManageRepository.save(rentalManage);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<RentalManage> findByStockIdAndStatusIn(String StockId, Long rentalId) {
        List<RentalManage> rentalAvailable = this.rentalManageRepository.findByStockIdAndStatusIn(StockId, rentalId);
        return rentalAvailable;
    }

    @Transactional
    public void update(Long id, RentalManageDto rentalManageDto) throws Exception {
        try {
            log.info("ここまで来た-1");
            log.info("EmployeeId："+rentalManageDto.getEmployeeId());
            Account account = this.accountRepository.findByEmployeeId(rentalManageDto.getEmployeeId()).orElse(null);
            log.info("ここまで来た0");
            if (account == null) {
                throw new Exception("Account not found.");
            }
            log.info("ここまで来た1");

            Stock stock = this.stockRepository.findById(rentalManageDto.getStockId()).orElse(null);
            if (stock == null) {
                throw new Exception("Stock not found.");
            }
            log.info("ここまで来た2");


            // 既存レコード取得
            RentalManage updateTargetrentalManage = this.rentalManageRepository.findById(id).orElse(null);
            //updateTargetrentalManage = setRentalStatusDate(updateTargetrentalManage, rentalManageDto.getStatus());

            if (updateTargetrentalManage == null) {
                throw new Exception("RentalManage record not found.");
            }
            log.info("ここまで来た3");

            updateTargetrentalManage.setAccount(account);
            updateTargetrentalManage.setExpectedRentalOn(rentalManageDto.getExpectedRentalOn());
            updateTargetrentalManage.setExpectedReturnOn(rentalManageDto.getExpectedReturnOn());
            updateTargetrentalManage.setStatus(rentalManageDto.getStatus());
            updateTargetrentalManage.setStock(stock);

            log.info("ここまで来た4");

            // データベースへの保存
            this.rentalManageRepository.save(updateTargetrentalManage);
        } catch (Exception e) {
            throw e;//例外をスローする？
        }
        log.info("ここまで来た5");
    }

  

    //レンタル日時、返却日時、キャンセル日時を更新した時間をデータベースに記録する処理
    private RentalManage setRentalStatusDate(RentalManage rentalManage, Integer status) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        if (status == RentalStatus.RENTALING.getValue()) {
            rentalManage.setRentaledAt(timestamp);
        } else if (status == RentalStatus.RETURNED.getValue()) {
            rentalManage.setReturnedAt(timestamp);
        } else if (status == RentalStatus.CANCELED.getValue()) {
            rentalManage.setCanceledAt(timestamp);
        }
        log.info("ここまで来た6");

        return rentalManage;
    }
}
