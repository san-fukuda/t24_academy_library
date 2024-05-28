package jp.co.metateam.library.model;
 
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
 
import org.springframework.format.annotation.DateTimeFormat;
 
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jp.co.metateam.library.values.RentalStatus;
import lombok.Getter;
import lombok.Setter;
 
/**
 * 貸出管理DTO
 */
@Getter
@Setter
public class RentalManageDto {
 
    private Long id;
 
    @NotEmpty(message="在庫管理番号は必須です")
    private String stockId;
 
    @NotEmpty(message="社員番号は必須です")
    private String employeeId;
 
    @NotNull(message="貸出ステータスは必須です")
    private Integer status;
 
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @NotNull(message="貸出予定日は必須です")
    private Date expectedRentalOn;
 
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @NotNull(message="返却予定日は必須です")
    private Date expectedReturnOn;
 
    private Timestamp rentaledAt;
 
    private Timestamp returnedAt;
 
    private Timestamp canceledAt;
 
    private Stock stock;
 
    private Account account;
 
 
    public Optional<String> isValidDateTime(Date expectedRentalOn , Date expectedReturnOn) {
        if (expectedRentalOn.compareTo(expectedReturnOn) >= 0) {
            return Optional.of("返却予定日は貸出予定日より後の日付を入力してください");
        }
        return Optional.empty();
    }
 
   
public Optional<String> isValidStatus(Integer preStatus) {
 
        String errorMassage = "貸出ステータスは「%s」から「%s」に変更できません";
 
        if(preStatus != this.status) {
           
            switch (preStatus) {
                case 0:
                    if (RentalStatus.RETURNED.getValue().equals(this.status)) {
                        return Optional.of (String.format(errorMassage, RentalStatus.RENT_WAIT.getText(), RentalStatus.RETURNED.getText()));
                    }
                    break;
                case 1:
                    if(RentalStatus.RENT_WAIT.getValue().equals(this.status)) {
                        return Optional.of (String.format(errorMassage, RentalStatus.RENTALING.getText(), RentalStatus.RENT_WAIT.getText()));
                    } else if (this.status == RentalStatus.CANCELED.getValue()) {
                        return Optional.of (String.format(errorMassage, RentalStatus.RENTALING.getText(), RentalStatus.CANCELED.getValue()));
                    }
                    break;
                case 2:
                    if(RentalStatus.RENT_WAIT.getValue().equals(this.status)){
                        return Optional.of (String.format(errorMassage, RentalStatus.RETURNED.getValue(), RentalStatus.RENT_WAIT.getValue()));
                    } else if(this.status == RentalStatus.RENTALING.getValue()){
                        return Optional.of (String.format(errorMassage, RentalStatus.RETURNED.getValue(), RentalStatus.RENTALING.getValue()));
                    } else if(this.status == RentalStatus.CANCELED.getValue()){
                        return Optional.of (String.format(errorMassage, RentalStatus.RETURNED.getValue(), RentalStatus.CANCELED.getValue()));
                    }
                    break;
                case 3:
                   if(RentalStatus.RENT_WAIT.getValue().equals(this.status)){
                       return Optional.of (String.format(errorMassage, RentalStatus.CANCELED.getValue(), RentalStatus.RENT_WAIT.getValue()));
                    } else if(this.status == RentalStatus.RENTALING.getValue()){
                       return Optional.of (String.format(errorMassage, RentalStatus.CANCELED.getValue(), RentalStatus.RENTALING.getValue()));
                    } else if(this.status == RentalStatus.RETURNED.getValue()){
                       return Optional.of (String.format(errorMassage, RentalStatus.CANCELED.getValue(), RentalStatus.RETURNED.getValue()));
                    }
                    break;
            }
        }
        return Optional.empty();
    }
} 