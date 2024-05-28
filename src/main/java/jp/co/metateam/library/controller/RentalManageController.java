package jp.co.metateam.library.controller;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import lombok.extern.log4j.Log4j2;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.RentalManage;
import java.util.List;
import jp.co.metateam.library.values.RentalStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.Stock;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
 
/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {
 
    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;
 
    @Autowired
    public RentalManageController(
        AccountService accountService,
        RentalManageService rentalManageService,
        StockService stockService
    ) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }
 
    /**
     * 貸出一覧画面初期表示
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List <RentalManage> rentalManageList = this.rentalManageService.findAll();
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);
        // 貸出一覧画面に遷移
        return "rental/index";
    }
 
    @GetMapping("/rental/add")
    public String add(Model model) {
        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findAll();
 
        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());
 
        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());
        }
 
        return "rental/add";
    }
   
    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, RedirectAttributes ra) {
        try {
            String dateError = this.findAvailableWithRentalDate(rentalManageDto, rentalManageDto.getStockId());

            if(dateError != null){
                result.addError(new FieldError("rentalManageDto", "expectedRentalOn", dateError));
                result.addError(new FieldError("rentalManageDto", "expectedReturnOn", dateError));
            }
 
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            this.rentalManageService.save(rentalManageDto);
 
            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());
 
            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.stockDto", result);
 
            return "redirect:/rental/add";
        }
    }

    //ここでSQLでDBからもってきた日付と入力された日付を比較する、これを上で使って被ってたら上でエラーを投げる
    public String findAvailableWithRentalDate(RentalManageDto rentalManageDto,  String stockId) {
        List<RentalManage> rentalAvailable = this.rentalManageService
        .findByStockIdAndStatusIn(rentalManageDto.getStockId());

        for(RentalManage exist : rentalAvailable) {
            if(exist.getExpectedRentalOn().compareTo(rentalManageDto.getExpectedReturnOn()) <= 0 && 
            rentalManageDto.getExpectedRentalOn().compareTo(exist.getExpectedReturnOn()) <= 0){
                return "選択された日付は登録済みの貸出情報と重複しています";
            }
        }

        return null;
    }
 
    //貸出編集機能
    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") String id, Model model) {
        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findAll();
 
        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
            RentalManageDto rentalManageDto = new RentalManageDto();

            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStockId(rentalManage.getStock().getId());
            rentalManageDto.setStatus(rentalManage.getStatus());
 
            model.addAttribute("rentalManageDto", rentalManageDto);
        }
       
        return "rental/edit";
    }

    @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") String id, @Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, RedirectAttributes ra, Model model) {
         //リクエストパスのidをString型で受け取る・@Validでバリデーションチェックをしけ結果をBindingResultに格納・RentalManageDtoオブジェクトを@ModelAttributeとして受け取る
         
            try {
 
                RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));//変更前の貸出情報
                //Integer preStatus = rentalManage.getStatus();
                Optional<String> statusError = rentalManageDto.isValidStatus(rentalManage.getStatus());//rentalManageDtoの貸出ステータスが有効かどうか
 
                if(statusError.isPresent()){
                    //Dtoで行った貸出ステータスのバリデーションチェックでエラーがあった場合
                    FieldError fieldError = new FieldError("rentalManageDto","status", statusError.get());
                    //statusErrorから取得したエラーメッセージをfieldErrorに入れる
                    result.addError(fieldError);
                    //resultにエラーの情報を入れる
                    throw new Exception("Validation error");
                    //エラーを投げる
                }

                long Id = Long.parseLong(id);

                String dateError = this.findAvailableWithRentalDate(rentalManageDto, Id);

                if(dateError != null){
                    result.addError(new FieldError("rentalManageDto", "expectedRentalOn", dateError));
                    result.addError(new FieldError("rentalManageDto", "expectedReturnOn", dateError));
                }

                if (result.hasErrors()) {
                    //resultにエラーの情報がある場合
                     throw new Exception("Validation error.");
                     //エラーを投げる
                }

               // 登録処理
               //指定されたidのrentalManageオブジェクトを更新
                rentalManageService.update(Long.valueOf(id), rentalManageDto);
                //更新された場合は指定されたリダイレクト先にデータを返す
                return "redirect:/rental/index";
 
            } catch (Exception e) {
                //エラーがあった場合はエラーメッセージを表示するようにする
                log.error(e.getMessage());
 
                ra.addFlashAttribute("rentalManageDto", rentalManageDto);
                ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);
 
                return String.format("redirect:/rental/%s/edit",id);//書籍編集画面に戻る
            }
        }

        //この処理はserviceに書くべき(できれば)
        //ここでSQLでDBからもってきた日付と入力された日付を比較する、これを上で使って被ってたら上でエラーを投げる
        public String findAvailableWithRentalDate(RentalManageDto rentalManageDto,  Long rentalId) {
            List<RentalManage> rentalAvailable = this.rentalManageService
            .findByStockIdAndStatusIn(rentalManageDto.getStockId(), rentalId);

            for(RentalManage exist : rentalAvailable) {
                if(exist.getExpectedRentalOn().compareTo(rentalManageDto.getExpectedReturnOn()) <= 0 && 
                rentalManageDto.getExpectedRentalOn().compareTo(exist.getExpectedReturnOn()) <= 0){
                    return "選択された日付は登録済みの貸出情報と重複しています";
                }
            }

            return null;
        }
    }

 
 
 
 