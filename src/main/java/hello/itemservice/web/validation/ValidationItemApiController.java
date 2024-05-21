package hello.itemservice.web.validation;


import hello.itemservice.web.validation.form.ItemSaveForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/validation/api/items")
public class ValidationItemApiController {

    // @ModelAttribute는 세밀하게 필드 단위로 세밀하게 적용돼서, 특정 필드에 타입이 맞지 않는 오류(바인딩)가 발생해도 나머지 필드는 정상 처리돼서 Validator를 사용한 검증도 적용 가능.
    // @RequestBody는 전체 객체 단위로 적용돼서 HttpMessageConverter의 작동이 성공해서 Item 객체가 만들어져야 Validator 적용 가능.
    // HttpMessageConverter 단계에서 JSON 데이터를 객체로 변경하지 못하면 이후 단계 자체가 진행이 안되고 예외가 터진다.(컨트롤러도 호출x, Validator도 적용x)

    @PostMapping("/add")
    public Object addItem(@RequestBody @Validated ItemSaveForm form, BindingResult bindingResult) {

        log.info("API 컨트롤러 호출");

        if (bindingResult.hasErrors()) {
            log.info("검증 오류 발생 errors = {}", bindingResult);
            return bindingResult.getAllErrors();    // JSON으로 반환됨.(@RestController 때문에 자바 객체가 -> JSON 객체로 바껴서)
        }

        log.info("성공 로직 실행");

        return form;
    }
}
