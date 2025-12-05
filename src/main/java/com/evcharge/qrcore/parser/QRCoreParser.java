package com.evcharge.qrcore.parser;

import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 二维码统一解析入口。
 * <p>
 * 设计目标：
 * 1. 对外只暴露一个静态方法 {@link #parse(String)}，业务层不关心具体解析规则。
 * 2. 内部维护一组有顺序的二维码解析器 {@link IQRCodeParser} 实现类，按顺序逐个尝试。
 * 3. 支持轻量的扩展：新增解析器只需要实现接口并在静态代码块中注册。
 * <p>
 * 工作流程（简化说明）：
 * 1. 校验入参二维码内容是否为空，为空则直接返回失败结果。
 * 2. 遍历解析器列表，调用 {@link IQRCodeParser#supports(String)} 判断当前解析器是否“能处理”该二维码内容。
 * 3. 若支持，则调用 {@link IQRCodeParser#parse(String)} 进行解析；
 * - 解析成功且 {@link QRContent#success} 为 true，则记录日志并直接返回该结果。
 * - 解析异常则记录错误日志，继续尝试下一个解析器。
 * 4. 若所有解析器都无法成功解析，则返回一个通用的失败 {@link QRContent}，并附带原始二维码内容。
 * <p>
 * 线程安全说明：
 * - {@link #PARSERS} 只在静态代码块中初始化和注册，运行期不再修改，因此可以视为只读。
 * - {@link #parse(String)} 方法只遍历只读列表，不修改全局状态，因此在多线程环境下是安全的。
 */
public class QRCoreParser {

    /**
     * 日志标签前缀，统一该类输出的日志标识。
     */
    private static final String TAG = "二维码解析器";

    /**
     * 按顺序匹配的解析器列表。
     * 使用 List（当前为 LinkedList）而不是 Set，是为了确保“解析器执行顺序”是可控且稳定的。
     * <p>
     * 顺序的重要性：
     * 1. 越“专用”的解析器应该放在前面，优先尝试。
     * 2. 越“通用”的解析器（例如纯文本解析）应该放在最后作为兜底。
     */
    private static final List<IQRCodeParser> PARSERS = new LinkedList<>();

    /*
     * 静态代码块：
     * 用于在类加载阶段注册所有内置的二维码解析器。
     * 如需扩展新的二维码格式，只需要：
     * 1. 新增一个实现了 IQRCodeParser 接口的类。
     * 2. 在此处按合适的位置调用 register(new XxxQRParser()) 即可。
     *
     * 注意：注册顺序非常关键。
     */
    static {
        // 帝能 充电备 二维码解析器
        register(new WindyDeviceQRParser());
        // 拓强4G电表 二维码解析器
        register(new TQ4GMeterQRParser());
        // 通用 URL 解析器：处理 http / https 开头的二维码
        register(new UrlQRParser());
        // 最后兜底：纯文本解析器，能解析“任何”内容
        register(new TextQRParser());
    }

    /**
     * 注册二维码解析器。
     * 通常仅在静态代码块中调用，用于初始化系统内置的解析器列表。
     * <p>
     * 扩展建议：
     * 如需按模块或插件方式动态注册解析器，可以将本方法的访问级别改为 public，
     * 并在系统启动阶段由配置或插件自动调用。
     *
     * @param parser 二维码解析器具体实现，不允许为 null
     */
    private static void register(IQRCodeParser parser) {
        // 这里简单做非空保护，避免由于误传 null 造成 NPE
        if (parser == null) {
            LogsUtil.warn(TAG, "尝试注册空的二维码解析器实例，被忽略");
            return;
        }
        PARSERS.add(parser);
    }

    /**
     * 对外唯一入口方法：
     * 传入二维码原始内容，返回统一结构的解析结果 {@link QRContent}。
     * <p>
     * 使用说明：
     * 1. 调用方只需提供二维码内容，不需要关心具体是设备码、URL 还是其他格式。
     * 2. 若解析成功，返回的 QRContent.success 为 true，
     * 同时 type、spu_code、device_code、data、raw 等字段会尽可能被填充。
     * 3. 若解析失败，返回的 QRContent.success 为 false，
     * 并携带错误原因 message 以及原始内容 raw。
     *
     * @param content 二维码原始内容（例如扫码得到的字符串），允许为空或空串
     * @return 统一封装后的二维码解析结果，不会返回 null
     */
    public static QRContent parse(String content) {
        // 1. 基础校验：内容为空时直接返回失败结果，避免后续解析器重复做空判断
        if (StringUtil.isEmpty(content)) {
            return QRContent.fail("二维码内容为空");
        }

        // 2. 遍历已注册的解析器，按顺序逐一尝试
        for (IQRCodeParser parser : PARSERS) {
            try {
                // 2.1 先判断当前解析器是否“支持”该内容
                //     支持说明该解析器认为自己有能力解析这种格式，可以进一步调用 parse。
                if (!parser.supports(content)) {
                    continue;
                }

                // 2.2 真正执行解析逻辑
                QRContent result = parser.parse(content);

                // 2.3 判定解析是否成功：
                //     - result 不为 null
                //     - result.success 为 true
                if (result != null && result.success) {
                    // 记录一条结构化的成功解析日志，方便排查线上二维码识别情况
                    LogsUtil.info(TAG, "解析成功 - 解析器：%s 类型：%s 设备号：%s 其他数据：%s"
                            , parser.getClass().getSimpleName()
                            , result.type
                            , result.device_code
                            , result.data == null ? null : result.data.toJSONString()
                            , result.content
                    );
                    return result;
                }

                // 若走到这里，说明当前解析器虽“支持”该内容，但解析结果为失败或空，
                // 按约定直接继续交给下一个解析器尝试，不在此处记录失败日志，
                // 避免产生过多无意义日志。
            } catch (Exception e) {
                // 2.4 单个解析器异常不应影响整体解析流程：
                //     - 打印错误日志
                //     - 继续尝试后续解析器
                LogsUtil.error(e, TAG, "解析器[%s]处理二维码内容时发生异常，content=%s"
                        , parser.getClass().getSimpleName()
                        , content
                );
            }
        }

        // 3. 所有解析器都未能成功解析该内容，记录一条兜底日志
        LogsUtil.info(TAG, "没有匹配合适的二维码解析器，原始内容：%s", content);

        // 4. 返回一个统一的失败对象，并把原始内容写回 raw 方便排查
        QRContent fail = QRContent.fail("没有匹配到合适的二维码解析器");
        fail.content = content;
        return fail;
    }
}
