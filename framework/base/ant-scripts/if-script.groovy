def condition = elements.condition
def commands
if (condition[0].eval()) {
 commands = elements.commands
} else {
 commands = elements.else
}
if (commands && !commands.isEmpty()) commands[0].execute()
