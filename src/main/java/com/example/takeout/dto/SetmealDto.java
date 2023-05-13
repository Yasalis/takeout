package com.example.takeout.dto;



import com.example.takeout.entity.Setmeal;
import com.example.takeout.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
