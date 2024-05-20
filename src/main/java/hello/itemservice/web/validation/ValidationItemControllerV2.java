package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(ValidationItemControllerV2.class);
    private final ItemRepository itemRepository;
    private final ItemValidator itemValidator;


    /**
     * 컨트롤러가 호출되기 전에 항상 불러져서 WebDataBinder가 내부적으로 만들어지고
     * itemValidator를 항상 넣어둠.(모든 컨트롤러가 호출되기 전 마다 얘가 먼저 만들어지고 검증기를 넣어둠.)
     * 해당 컨트롤러에만 적용이 되고 글로벌한 적용은 따로 해줘야 함.(Application에 WebMvcConfigure implements 해주고 오버라이딩 해주면 됨.)
     * 그 다음 ItemValidator의 supports를 먼저 수행함.
     */
    @InitBinder
    public void init(WebDataBinder dataBinder) {
        dataBinder.addValidators(itemValidator);
    }



    @GetMapping
    public String items(Model model) {
        List<Item> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "validation/v2/items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/item";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Item());
        return "validation/v2/addForm";
    }



/*  // 이 V1 메서드는 사용자가 잘못 된 값을 입력한 뒤, 그 값이 다 사라져버림.
    // 검증할 객체(Item) 뒤에 BindingResult를 넣어야 함.
    // @ModelAttribute에 바인딩 시 타입 오류가 발생 했을 때,
    // BindingResult가 없다면? : 400오류가 발생하면서 컨트롤러가 호출되지 않고, 오류 페이지로 이동한다.(400 status)
    // BindingResult가 있다면? : 오류 정보('FieldError')를 BindingResult에 담아서 컨트롤러를 정상 호출한다.
    @PostMapping("/add")
    public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        // objectName : @ModelAttribute 이름(검증할 객체 이름)
        // field : 검증할 필드
        // defaultMessage : 오류 기본 메시지
        if (!StringUtils.hasText(item.getItemName())) { // itemName에 글자가 없으면(상품 입력 폼에서 상품 이름을 공란으로 두었을 때)
            bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다"));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", "가격 x 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice)); // 글로벌 오류
            }
        }

        // 검증에 실패하면 다시 입력 폼으로(검증 오류 메시지가 하나라도 있으면 model에 errors를 담고 다시 추가(/add) 페이지를 띄워줌)
        if (bindingResult.hasErrors()) {
            log.info("errors = {} ", bindingResult);
//            model.addAttribute("errors", errors); BindingResult는 스프링이 자동으로 모델에 담아줌
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }*/




    /*// 컨트롤러가 호출이 되면서 @ModelAttribute에 담긴 값을 저장하기가 어렵지만, newFieldError()의 rejectedValue를 통해 저장할 수 있다.
    // V2 메서드는 스프링이 기본으로 제공하는 오류가 클라이언트에게도 보여지는 문제가 있음.
    @PostMapping("/add")
    public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        // objectName : @ModelAttribute 이름(검증할 객체 이름)
        // field : 검증할 필드
        // rejectedValue : 사용자가 입력한 값(거절된 값)
        // defaultMessage : 오류 기본 메시지
        // codes : 메시지 코드 -> 메시지, 국제화처럼 에러 메시지를 한 곳에 모아 둬서 사용 가능
        // arguments : 메시지에서 사용하는 인자 -> 메시지, 국제화처럼 에러 메시지를 한 곳에 모아 둬서 사용 가능
        if (!StringUtils.hasText(item.getItemName())) { // itemName에 글자가 없으면(상품 입력 폼에서 상품 이름을 공란으로 두었을 때)
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, null, null, "상품 이름은 필수입니다.")); // bindingFailure : 필드에 값이 바인딩 자체가 안 됐는지 여부(타입 오류 같은 바인딩 실패인지, 검증 실패인지 구분 값)
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null, "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null, "수량은 최대 9,999 까지 허용합니다"));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", null, null, "가격 x 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로(검증 오류 메시지가 하나라도 있으면 model에 errors를 담고 다시 추가(/add) 페이지를 띄워줌)
        if (bindingResult.hasErrors()) {
            log.info("errors = {} ", bindingResult);
//            model.addAttribute("errors", errors); BindingResult는 스프링이 자동으로 모델에 담아줌
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
*/




    // V3 메서드는 오류 메시지를 메시지, 국제화 마냥 한 버전
/*    @PostMapping("/add")
    public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        log.info("objectName = {}", bindingResult.getObjectName());
        log.info("target = {}", bindingResult.getTarget());

        // 검증 로직
        // objectName : @ModelAttribute 이름(검증할 객체 이름)
        // field : 검증할 필드
        // rejectedValue : 사용자가 입력한 값(거절된 값)
        // defaultMessage : 오류 기본 메시지
        // codes : 메시지 코드 -> 메시지, 국제화처럼 에러 메시지를 한 곳에 모아 둬서 사용 가능
        // arguments : 메시지에서 사용하는 인자 -> 메시지, 국제화처럼 에러 메시지를 한 곳에 모아 둬서 사용 가능
        if (!StringUtils.hasText(item.getItemName())) { // itemName에 글자가 없으면(상품 입력 폼에서 상품 이름을 공란으로 두었을 때)
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "itemName", item.getItemName(), false, new String[]{"required.item.itemName", "배열인 이유는 첫 번째 거를 못 찾았을 땐 이거 쓰라고"}, null, "둘 다 default면 이게 나와"));
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price", "required.default"}, new Object[]{1000, 1000000}, null));
        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, null));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000, resultPrice}, null));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로(검증 오류 메시지가 하나라도 있으면 model에 errors를 담고 다시 추가(/add) 페이지를 띄워줌)
        if (bindingResult.hasErrors()) {
            log.info("errors = {} ", bindingResult);
//            model.addAttribute("errors", errors); BindingResult는 스프링이 자동으로 모델에 담아줌
            return "validation/v2/addForm";
        }*/




        /*// V4 메서드는 new FieldError, new ObjectError의 중복을 제거한 BindingResult.rejectValue(), BindingResult.reject() 버전
        @PostMapping("/add")
        public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

            // 바인딩 실패 시(앞에서 해줌으로써 사용자 페이지에 한 가지의 오류만 나옴, 보통 앞에서 함)
            if (bindingResult.hasErrors()) {
                log.info("errors = {} ", bindingResult);
                return "validation/v2/addForm";
            }

            log.info("objectName = {}", bindingResult.getObjectName());
            log.info("target = {}", bindingResult.getTarget());

            // 검증 로직
            // BindingResult가 제공하는 rejectValue(), reject()를 사용하면 FieldError, ObjectError 를 직접 생성하지 않고, 검증 오류를 다룰 수 있다.
            // 알아서 BindingResult가 target(Item Object)를 잡아줌. log.info() 찍어보면 암.
//            ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required"); 이거나 바로 아래나 똑같음.(Empty 같은 공백 단순한 기능만 제공)

            if (!StringUtils.hasText(item.getItemName())) { // itemName에 글자가 없으면(상품 입력 폼에서 상품 이름을 공란으로 두었을 때)
                bindingResult.rejectValue("itemName", "required");  // 코드를 만질 필요 없이 properties만 수정해서 전체 메시지 관리 가능(스프링이 MessageCodesResolver로 이런 기능을 지원)
//                new String[]{"requied.item.itemName", "required"};
            }

            if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
                bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000} ,null);
            }
            if (item.getQuantity() == null || item.getQuantity() >= 9999) {
                bindingResult.rejectValue("quantity", "max", new Object[]{9999}, null);
            }

            // 특정 필드가 아닌 복합 룰 검증
            if (item.getPrice() != null && item.getQuantity() != null) {
                int resultPrice = item.getPrice() * item.getQuantity();
                if(resultPrice < 10000) {
                    bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
                }
            }

            // 검증에 실패하면 다시 입력 폼으로(검증 오류 메시지가 하나라도 있으면 model에 errors를 담고 다시 추가(/add) 페이지를 띄워줌)
            if (bindingResult.hasErrors()) {
                log.info("errors = {} ", bindingResult);
//            model.addAttribute("errors", errors); BindingResult는 스프링이 자동으로 모델에 담아줌
                return "validation/v2/addForm";
            }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
*/




    /*@PostMapping("/add")
    public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        itemValidator.validate(item, bindingResult);

        if (bindingResult.hasErrors()) {
            log.info("errors = {} ", bindingResult);
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
*/


    /**
     * @Validated나 @Valid나 같음
     * @Validate : 스프링 전용 검증 어노테이션
     * @Valid : 자바 표준(javax) 검증 어노테이션(의존관계 추가 필요)
     * 이 컨트롤러가 호출되기 전에 Validator가 먼저 호출이 되기 때문에 TypeMissMatch || Field 에러를 선택해서 보여 줄 수 없게됨.(둘 다 틀리면 둘 다 떠 버림)
     * 이걸 위해선 먼저 호출 되는 Validator에서 BindindResult로 먼저 검증 해주면 됨.
     */
    @PostMapping("/add")
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        if (bindingResult.hasErrors()) {
            log.info("errors = {} ", bindingResult);
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }



    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute Item item) {
        itemRepository.update(itemId, item);
        return "redirect:/validation/v2/items/{itemId}";
    }

}

